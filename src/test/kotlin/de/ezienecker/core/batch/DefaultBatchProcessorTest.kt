package de.ezienecker.core.batch

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay

class DefaultBatchProcessorTest : FunSpec({

    val batchProcessor = DefaultBatchProcessor<Int, List<Int>>()

    test("processBatch should process items in sequential batches") {
        // Given
        val items = (1..100).toList()
        val batchSize = 25

        // When
        val results = batchProcessor.processBatch(items, batchSize) { batch ->
            batch.map { it * 2 }
        }

        // Then
        results shouldHaveSize 4
        results.flatten() shouldBe items.map { it * 2 }
    }

    test("processBatch should handle empty list") {
        // Given
        val items = emptyList<Int>()

        // When
        val results = batchProcessor.processBatch(items, 10) { batch ->
            batch.map { it * 2 }
        }

        // Then
        results shouldHaveSize 0
    }

    test("processParallelBatch should process items in parallel batches") {
        // Given
        val items = (1..100).toList()
        val batchSize = 20
        val concurrency = 3

        // When
        val results = batchProcessor.processParallelBatch(items, batchSize, concurrency) { batch ->
            delay(10) // Simulate async work
            batch.map { it * 2 }
        }

        // Then
        results shouldHaveSize 5
        results.flatten() shouldBe items.map { it * 2 }
    }

    test("processParallelBatch should respect concurrency limit") {
        // Given
        val items = (1..50).toList()
        val batchSize = 10
        val concurrency = 2
        var maxConcurrent = 0
        var currentConcurrent = 0

        // When
        batchProcessor.processParallelBatch(items, batchSize, concurrency) { batch ->
            currentConcurrent++
            if (currentConcurrent > maxConcurrent) {
                maxConcurrent = currentConcurrent
            }
            delay(50) // Simulate work
            currentConcurrent--
            batch
        }

        // Then
        maxConcurrent shouldBe concurrency
    }
})

