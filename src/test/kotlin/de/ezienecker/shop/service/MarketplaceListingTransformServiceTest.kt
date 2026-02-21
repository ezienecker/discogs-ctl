package de.ezienecker.shop.service

import de.ezienecker.core.infrastructure.discogs.marketplace.MarketplaceSeller
import de.ezienecker.wantlist.service.MarketplaceListingTransformService
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class MarketplaceListingTransformServiceTest : StringSpec({

    val marketplaceListingTransformService = MarketplaceListingTransformService()

    "should transform single listing from real HTML" {
        val releaseId = 42424242L
        val listingHtml = """
            <html>
            <body>
            <div id="pjax_container">
                <table>
                    <tbody>
                        <tr class="shortcut_navigable" data-release-id="42424242">
                            <td class="item_picture as_float">
                                <div class="hide_mobile community_data_text">
                                    <div class="community_rating">
                                        Rated: <strong>4.80</strong>
                                    </div>
                                </div>
                            </td>
                            <td class="item_description">
                                <strong>
                                    <a href="/sell/item/2085508604" class="item_description_title" data-followable="true">John Doe and Band - 1995 (LP, Ltd, Mixtape, Whi)</a>
                                </strong>
                                <br />
                                <p class="hide_mobile label_and_cat">
                                    <span class="mplabel">Label:</span><a href="https://www.discogs.com/label/1683917-Air-Vinyl-Records">Air Vinyl Records</a><br />
                                    <span class="mplabel">Cat#:</span>
                                    <span class="item_catno">AV051-LP</span>
                                    <br />
                                </p>
                                <p class="item_condition">
                                    <span class="mplabel condition-label-desktop">Media Condition:</span>
                                    <span class="mplabel condition-label-mobile">Media:</span>
                                    <span>
                                        Mint (M)
                                        <span class="has-tooltip" role="note" tabindex="0">
                                            <i class="icon icon-info-circle muted" role="img" aria-hidden="true"></i>
                                            <span class="tooltip multi-line-tooltip sr-only" role="tooltip">
                                                <span class="tooltip-inner">
                                                    Absolutely perfect in every way. Certainly never been played. Should be used sparingly as a grade.            
                                                </span>
                                            </span>
                                        </span>
                                    </span>
                                    <br />
                                    <span class="mplabel condition-label-desktop">Sleeve Condition:</span>
                                    <span class="mplabel condition-label-mobile">Sleeve:</span>
                                    <span class="item_sleeve_condition">Mint (M)</span>
                                    <br />
                                </p>
                            </td>
                            <td class="seller_info">
                                <ul>
                                    <li>
                                        <div class="seller_block">
                                            <span class="mplabel seller_label">Seller:</span>
                                            <strong><a href="/seller/John Doe/profile">John Doe</a></strong>
                                        </div>
                                    </li>
                                    <li>
                                        <span class="star_rating" alt="John Doe rating 5.0 stars out of 5" role="img" aria-label="John Doe rating 5.0 out of 5">
                                            <i role="img" aria-hidden="true" class="icon icon-star"></i>
                                        </span> 
                                        <strong>100.0%</strong><span>,</span>
                                        <a href="/sell/seller_feedback/John Doe" target="_top" class="section_link">240 ratings</a>
                                    </li>
                                    <li><span class="mplabel">Ships From:</span>United States</li>
                                </ul>
                            </td>
                            <td class="item_price hide_mobile">
                                <div>
                                    <span class="price" data-currency=USD data-pricevalue=24.99>$24.99</span>
                                    <br />
                                    <span class="item_shipping">
                                        +$24.00
                                        <button class="show-shipping-methods" data-seller-username="John Doe" data-seller-id="966843">shipping</button>
                                    </span>
                                </div>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
            </body>
            </html>
        """.trimIndent()

        val result = marketplaceListingTransformService.transformListings(releaseId, listingHtml)

        result shouldHaveSize 1
        result[0].releaseId shouldBe releaseId
        result[0].title shouldBe "John Doe and Band - 1995 (LP, Ltd, Mixtape, Whi)"
        result[0].resourceUrl shouldBe "https://www.discogs.com/sell/release/42424242"
        result[0].mediaCondition shouldBe "Mint (M)"
        result[0].sleeveCondition shouldBe "Mint (M)"
        result[0].price shouldBe "$24.99"
        result[0].seller shouldBe MarketplaceSeller("John Doe")
        result[0].shippingLocation shouldBe "United States"
    }

    "should transform multiple listings from real HTML" {
        val releaseId = 42424242L
        val listingHtml = """
            <html>
            <body>
            <div id="pjax_container">
                <table>
                    <tbody>
                        <tr class="shortcut_navigable" data-release-id="42424242">
                            <td class="item_picture as_float"></td>
                            <td class="item_description">
                                <strong>
                                    <a href="/sell/item/2085508604" class="item_description_title">John Doe and Band - 1995 (LP, Ltd, Mixtape, Whi)</a>
                                </strong>
                                <p class="item_condition">
                                    <span class="mplabel condition-label-desktop">Media Condition:</span>
                                    <span>Mint (M)</span>
                                    <br />
                                    <span class="mplabel condition-label-desktop">Sleeve Condition:</span>
                                    <span class="item_sleeve_condition">Mint (M)</span>
                                </p>
                            </td>
                            <td class="seller_info">
                                <ul>
                                    <li>
                                        <div class="seller_block">
                                            <strong><a href="/seller/John Doe/profile">John Doe</a></strong>
                                        </div>
                                    </li>
                                    <li></li>
                                    <li>United States</li>
                                </ul>
                            </td>
                            <td class="item_price hide_mobile">
                                <div>
                                    <span class="price">$24.99</span>
                                </div>
                            </td>
                        </tr>
                        <tr class="shortcut_navigable unavailable" data-release-id="42424242">
                            <td class="item_picture as_float"></td>
                            <td class="item_description">
                                <strong>
                                    <a href="/sell/item/2131463900" class="item_description_title">John Doe and Band - 1995 (LP, Ltd, Mixtape, Whi)</a>
                                </strong>
                                <p class="item_condition">
                                    <span class="mplabel condition-label-desktop">Media Condition:</span>
                                    <span>Mint (M)</span>
                                    <br />
                                    <span class="mplabel condition-label-desktop">Sleeve Condition:</span>
                                    <span class="item_sleeve_condition">Mint (M)</span>
                                </p>
                            </td>
                            <td class="seller_info">
                                <ul>
                                    <li>
                                        <div class="seller_block">
                                            <strong><a href="/seller/Jane Doe/profile">Jane Doe</a></strong>
                                        </div>
                                    </li>
                                    <li></li>
                                    <li>United States</li>
                                </ul>
                            </td>
                            <td class="item_price hide_mobile">
                                <div>
                                    <span class="price">$50.00</span>
                                </div>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
            </body>
            </html>
        """.trimIndent()

        val result = marketplaceListingTransformService.transformListings(releaseId, listingHtml)

        result shouldHaveSize 2

        // First listing
        result[0].releaseId shouldBe releaseId
        result[0].title shouldBe "John Doe and Band - 1995 (LP, Ltd, Mixtape, Whi)"
        result[0].seller shouldBe MarketplaceSeller("John Doe")
        result[0].price shouldBe "$24.99"
        result[0].shippingLocation shouldBe "United States"

        // Second listing
        result[1].releaseId shouldBe releaseId
        result[1].title shouldBe "John Doe and Band - 1995 (LP, Ltd, Mixtape, Whi)"
        result[1].seller shouldBe MarketplaceSeller("Jane Doe")
        result[1].price shouldBe "$50.00"
        result[1].shippingLocation shouldBe "United States"
    }

    "should handle empty table body" {
        val releaseId = 42424242L
        val listingHtml = """
            <html>
               <body>
                  <div id="pjax_container" aria-live="polite">
                     <div class="hide-desktop">
                        <nav aria-label="Pagination" class="pagination top ">
                           <form action="/sell/release/35886547" method="get" data-section="general">
                              <strong class="pagination_total">
                              0 – 0 of 0    </strong>
                              <div class="pagination_controls multiple_filters">
                                 <label for="sort_top" class="sort_top">Sort</label>
                                 <select name="sort" id="sort_top">
                                    <option value="listed,desc">Listed Newest</option>
                                    <option value="listed,asc">Listed Oldest</option>
                                    <option value="condition,desc">Condition (M)-(P)</option>
                                    <option value="condition,asc">Condition (P)-(M)</option>
                                    <option value="seller,asc">Seller A-Z</option>
                                    <option value="seller,desc">Seller Z-A</option>
                                    <option value="price,asc" selected="selected">Price Lowest</option>
                                    <option value="price,desc">Price Highest</option>
                                 </select>
                                 <br class="hide-desktop">
                                 <label class="hide_mobile" for="limit_top">Show<span class="sr-only"> items per page</span></label>
                                 <select class="hide_mobile" name="limit" id="limit_top">
                                    <option value="25" selected="selected">25</option>
                                    <option value="50">50</option>
                                    <option value="100">100</option>
                                    <option value="250">250</option>
                                 </select>
                                 <noscript><button type="submit">Refresh</button></noscript>
                              </div>
                           </form>
                        </nav>
                     </div>
                     <table class="table_block mpitems push_down table_responsive">
                        <thead>
                           <tr>
                              <th class="marketplace_available_sorts" colspan="2">Sort By:    <a href="/sell/release/35886547?sort=listed%2Cdesc" class="sortable-link "><span class="link-text">Listed</span></a>,    <a href="/sell/release/35886547?sort=condition%2Cdesc" class="sortable-link "><span class="link-text">Condition</span></a>, Artist, Title, Label</th>
                              <th class="seller_info_header">    <a href="/sell/release/35886547?sort=seller%2Casc" class="sortable-link "><span class="link-text">Seller</span></a></th>
                              <th class="price_header">    <a href="/sell/release/35886547?sort=price%2Cdesc" class="sortable-link  sortable_link_selected" title="sorted ascending"><span class="link-text">Price</span>&nbsp;<i class="icon icon-chevron-up" role="img" aria-hidden="true"></i></a></th>
                              <th><span class="sr-only">Actions</span></th>
                           </tr>
                        </thead>
                        <tbody>
                        </tbody>
                     </table>
                     <nav aria-label="Pagination" class="pagination bottom ">
                        <form action="/sell/release/35886547" method="get" data-section="general">
                           <strong class="pagination_total" data-gtm-vis-recent-on-screen31944549_815="98" data-gtm-vis-first-on-screen31944549_815="98" data-gtm-vis-total-visible-time31944549_815="100" data-gtm-vis-has-fired31944549_815="1">
                           0 – 0 of 0    </strong>
                           <div class="pagination_controls multiple_filters">
                              <div class="hide_mobile">
                                 <label for="sort_bottom" class="sort_bottom">Sort</label>
                                 <select name="sort" id="sort_bottom">
                                    <option value="listed,desc">Listed Newest</option>
                                    <option value="listed,asc">Listed Oldest</option>
                                    <option value="condition,desc">Condition (M)-(P)</option>
                                    <option value="condition,asc">Condition (P)-(M)</option>
                                    <option value="seller,asc">Seller A-Z</option>
                                    <option value="seller,desc">Seller Z-A</option>
                                    <option value="price,asc" selected="selected">Price Lowest</option>
                                    <option value="price,desc">Price Highest</option>
                                 </select>
                                 <br class="hide-desktop">
                              </div>
                              <label class="hide_mobile" for="limit_bottom">Show<span class="sr-only"> items per page</span></label>
                              <select class="hide_mobile" name="limit" id="limit_bottom">
                                 <option value="25" selected="selected">25</option>
                                 <option value="50">50</option>
                                 <option value="100">100</option>
                                 <option value="250">250</option>
                              </select>
                              <noscript><button type="submit">Refresh</button></noscript>
                           </div>
                        </form>
                     </nav>
                     <div class="bottom-statement">
                        Each seller assumes all responsibility for their listings. Sellers certify that they only offer products or services that comply with applicable laws and regulations. Each seller is responsible for the sale of their items and for managing any issues arising out of or in connection with the contract for sale between the seller and the buyer.
                     </div>
                  </div>
               </body>
            </html>
        """.trimIndent()

        val result = marketplaceListingTransformService.transformListings(releaseId, listingHtml)

        result shouldHaveSize 0
    }

    "should handle missing media condition" {
        val releaseId = 42424242L
        val listingHtml = """
            <html>
            <body>
            <div id="pjax_container">
                <table>
                    <tbody>
                        <tr class="shortcut_navigable">
                            <td class="item_picture as_float"></td>
                            <td class="item_description">
                                <strong>
                                    <a href="/sell/item/123" class="item_description_title">Test Album</a>
                                </strong>
                                <p class="item_condition">
                                    <span class="mplabel condition-label-desktop">Sleeve Condition:</span>
                                    <span class="item_sleeve_condition">Very Good Plus (VG+)</span>
                                </p>
                            </td>
                            <td class="seller_info">
                                <ul>
                                    <li>
                                        <div class="seller_block">
                                            <strong><a href="/seller/test/profile">TestSeller</a></strong>
                                        </div>
                                    </li>
                                    <li></li>
                                    <li>Germany</li>
                                </ul>
                            </td>
                            <td class="item_price hide_mobile">
                                <div>
                                    <span class="price">€15.00</span>
                                </div>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
            </body>
            </html>
        """.trimIndent()

        val result = marketplaceListingTransformService.transformListings(releaseId, listingHtml)

        result shouldHaveSize 1
        result[0].mediaCondition shouldBe ""
        result[0].sleeveCondition shouldBe "Very Good Plus (VG+)"
    }

    "should handle missing sleeve condition" {
        val releaseId = 42424242L
        val listingHtml = """
            <html>
            <body>
            <div id="pjax_container">
                <table>
                    <tbody>
                        <tr class="shortcut_navigable">
                            <td class="item_picture as_float"></td>
                            <td class="item_description">
                                <strong>
                                    <a href="/sell/item/456" class="item_description_title">Another Test Album</a>
                                </strong>
                                <p class="item_condition">
                                    <span class="mplabel condition-label-desktop">Media Condition:</span>
                                    <span class="mplabel condition-label-mobiel">Media:</span>
                                    <span>Near Mint (NM or M-)</span>
                                </p>
                            </td>
                            <td class="seller_info">
                                <ul>
                                    <li>
                                        <div class="seller_block">
                                            <strong><a href="/seller/test2/profile">AnotherSeller</a></strong>
                                        </div>
                                    </li>
                                    <li></li>
                                    <li>France</li>
                                </ul>
                            </td>
                            <td class="item_price hide_mobile">
                                <div>
                                    <span class="price">€20.50</span>
                                </div>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
            </body>
            </html>
        """.trimIndent()

        val result = marketplaceListingTransformService.transformListings(releaseId, listingHtml)

        result shouldHaveSize 1
        result[0].mediaCondition shouldBe "Near Mint (NM or M-)"
        result[0].sleeveCondition shouldBe ""
    }
})
