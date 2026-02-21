package de.ezienecker.core.batch

/**
 * Interface for batch processing operations.
 * Allows different domains to execute batch operations with custom processing logic.
 *
 * @param T The type of items to be processed
 * @param R The type of result returned from processing
 */
interface BatchProcessor<T, R> {

    /**
     * Processes a collection of items in batches.
     *
     * @param items The items to process
     * @param batchSize The number of items to process in each batch
     * @param processor The function to apply to each batch of items
     * @return A list of results from processing all batches
     */
    suspend fun processBatch(
        items: List<T>,
        batchSize: Int = DEFAULT_BATCH_SIZE,
        processor: suspend (List<T>) -> R
    ): List<R>

    /**
     * Processes a collection of items in parallel batches.
     *
     * @param items The items to process
     * @param batchSize The number of items to process in each batch
     * @param concurrency The number of batches to process concurrently
     * @param processor The function to apply to each batch of items
     * @return A list of results from processing all batches
     */
    suspend fun processParallelBatch(
        items: List<T>,
        batchSize: Int = DEFAULT_BATCH_SIZE,
        concurrency: Int = DEFAULT_CONCURRENCY,
        processor: suspend (List<T>) -> R
    ): List<R>

    companion object {
        const val DEFAULT_BATCH_SIZE = 50
        const val DEFAULT_CONCURRENCY = 5
    }
}
