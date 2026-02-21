package de.ezienecker.core.batch

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

class DefaultBatchProcessor<T, R> : BatchProcessor<T, R> {

    override suspend fun processBatch(
        items: List<T>,
        batchSize: Int,
        processor: suspend (List<T>) -> R
    ): List<R> = coroutineScope {
        items.chunked(batchSize)
            .map { batch -> processor(batch) }
    }

    override suspend fun processParallelBatch(
        items: List<T>,
        batchSize: Int,
        concurrency: Int,
        processor: suspend (List<T>) -> R
    ): List<R> = coroutineScope {
        val semaphore = Semaphore(concurrency)

        items.chunked(batchSize)
            .map { batch ->
                async {
                    semaphore.withPermit {
                        processor(batch)
                    }
                }
            }
            .awaitAll()
    }
}
