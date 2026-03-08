package de.ezienecker.wantlist.service

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Element
import de.ezienecker.core.infrastructure.discogs.marketplace.MarketplaceListing
import de.ezienecker.core.infrastructure.discogs.marketplace.MarketplaceSeller
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

class MarketplaceListingTransformService {

    fun transformListings(releaseId: Long, marketplaceListingsHtml: String): List<MarketplaceListing> {
        val doc = Ksoup.parse(html = marketplaceListingsHtml)
        val medianPriceIndicator = getMedianPriceIndicator(doc)
        return doc.select("#pjax_container > table > tbody > tr").map {
            logger.trace { "Starting to transform listing for releaseId: [$releaseId]." }

            MarketplaceListing(
                releaseId = releaseId,
                title = getTitle(it),
                resourceUrl = "https://www.discogs.com/sell/release/$releaseId",
                mediaCondition = getMediaCondition(it),
                sleeveCondition = getSleeveCondition(it),
                price = getPrice(it),
                medianPriceIndicator = medianPriceIndicator,
                seller = MarketplaceSeller(getSellerName(it)),
                shippingLocation = getShippingLocation(it),
            ).also { releaseListing ->
                logger.debug { "Successfully transformed listing for release: [$releaseListing]." }
            }
        }
    }

    private fun getTitle(listingElement: Element): String {
        return listingElement.select(".item_description")
            .select("strong > a")
            .text()
    }

    private fun getSellerName(listingElement: Element): String {
        return listingElement.select(".seller_info")
            .select("ul > li > .seller_block > strong > a")
            .text()
    }

    private fun getShippingLocation(listingElement: Element): String {
        return (listingElement.select(".seller_info")
            .select("ul > li:nth-of-type(3n)")
            .first() as Element).ownText()
    }

    private fun getMediaCondition(listingElement: Element): String {
        return listingElement.select(".item_description")
            .select("p.item_condition > span:nth-child(3)")
            .first()?.ownText() ?: ""
    }

    private fun getSleeveCondition(listingElement: Element): String {
        return listingElement.select(".item_description")
            .select(".item_sleeve_condition")
            .text()
    }

    private fun getPrice(listingElement: Element): String {
        return listingElement.select(".item_price")
            .select("div > span.price")
            .text()
    }

    private fun getMedianPriceIndicator(doc: Element): String {
        return doc.select(".mprelease_info > .right ul.last > li")
            .firstOrNull { li ->
                li.select("span").any { span -> span.ownText() == "Median:" }
            }?.ownText()?.trim() ?: ""
    }
}
