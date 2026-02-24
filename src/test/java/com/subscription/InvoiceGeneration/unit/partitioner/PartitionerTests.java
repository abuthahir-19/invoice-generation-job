package com.subscription.InvoiceGeneration.unit.partitioner;

import com.subscription.InvoiceGeneration.config.InvoiceGeneratorConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.batch.core.partition.Partitioner;
import org.springframework.batch.infrastructure.item.ExecutionContext;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Customer Partitioner
 * Verifies partition strategy for parallel processing
 */

class PartitionerTests {

    private Partitioner partitioner;

    @BeforeEach
    void setUp() {
        InvoiceGeneratorConfig config = new InvoiceGeneratorConfig();
        partitioner = config.customerPartitioner();
    }

    // ==================== Partition Creation Tests ====================

    @Test
    @DisplayName("Partitioner - Should create correct number of partitions")
    void testPartitionCount() {
        // Given
        int gridSize = 4;

        // When
        Map<String, ExecutionContext> partitions = partitioner.partition(gridSize);

        // Then
        assertNotNull(partitions);
        assertEquals(gridSize, partitions.size(), "Should create exactly 4 partitions");
    }

    @Test
    @DisplayName("Partitioner - Should create partitions with correct names")
    void testPartitionNames() {
        // Given
        int gridSize = 4;

        // When
        Map<String, ExecutionContext> partitions = partitioner.partition(gridSize);

        // Then
        assertTrue(partitions.containsKey("partition0"));
        assertTrue(partitions.containsKey("partition1"));
        assertTrue(partitions.containsKey("partition2"));
        assertTrue(partitions.containsKey("partition3"));
    }

    @Test
    @DisplayName("Partitioner - Should divide range evenly")
    void testPartitionRangeDivision() {
        // Given
        int gridSize = 4;
        long minId = 1;
        long maxId = 10000000;
        long expectedRangeSize = (maxId - minId) / gridSize;

        // When
        Map<String, ExecutionContext> partitions = partitioner.partition(gridSize);

        // Then - Verify each partition has correct range
        ExecutionContext partition0 = partitions.get("partition0");
        assertEquals(1L, partition0.getLong("minId"));
        assertEquals(1L + expectedRangeSize - 1, partition0.getLong("maxId"));

        ExecutionContext partition1 = partitions.get("partition1");
        assertEquals(1L + expectedRangeSize, partition1.getLong("minId"));
        assertEquals(1L + (2 * expectedRangeSize) - 1, partition1.getLong("maxId"));
    }

    @Test
    @DisplayName("Partitioner - Last partition should include remaining range")
    void testLastPartitionIncludesRemainder() {
        // Given
        int gridSize = 4;
        long maxId = 10000000;

        // When
        Map<String, ExecutionContext> partitions = partitioner.partition(gridSize);

        // Then - Last partition should end at maxId
        ExecutionContext lastPartition = partitions.get("partition3");
        assertEquals(maxId, lastPartition.getLong("maxId"),
                "Last partition should include all remaining IDs up to maxId");
    }

    @Test
    @DisplayName("Partitioner - Should handle single partition")
    void testSinglePartition() {
        // Given
        int gridSize = 1;
        long minId = 1;
        long maxId = 10000000;

        // When
        Map<String, ExecutionContext> partitions = partitioner.partition(gridSize);

        // Then
        assertEquals(1, partitions.size());
        ExecutionContext partition = partitions.get("partition0");
        assertEquals(minId, partition.getLong("minId"));
        assertEquals(maxId, partition.getLong("maxId"));
    }

    @Test
    @DisplayName("Partitioner - Should handle many partitions")
    void testManyPartitions() {
        // Given
        int gridSize = 100;

        // When
        Map<String, ExecutionContext> partitions = partitioner.partition(gridSize);

        // Then
        assertEquals(100, partitions.size());
        // Verify first and last
        assertTrue(partitions.containsKey("partition0"));
        assertTrue(partitions.containsKey("partition99"));
    }

    // ==================== Partition Range Tests ====================

    @Test
    @DisplayName("Partitioner - Partitions should not overlap")
    void testPartitionsDoNotOverlap() {
        // Given
        int gridSize = 4;

        // When
        Map<String, ExecutionContext> partitions = partitioner.partition(gridSize);

        // Then - Verify no overlaps
        ExecutionContext partition0 = partitions.get("partition0");
        ExecutionContext partition1 = partitions.get("partition1");

        long partition0Max = partition0.getLong("maxId");
        long partition1Min = partition1.getLong("minId");

        assertEquals(partition0Max + 1, partition1Min,
                "Next partition should start where previous ends + 1");
    }

    @Test
    @DisplayName("Partitioner - All partitions should cover full range")
    void testPartitionsCoverFullRange() {
        // Given
        int gridSize = 4;
        long minId = 1;
        long maxId = 10000000;

        // When
        Map<String, ExecutionContext> partitions = partitioner.partition(gridSize);

        // Then
        ExecutionContext firstPartition = partitions.get("partition0");
        ExecutionContext lastPartition = partitions.get("partition3");

        assertEquals(minId, firstPartition.getLong("minId"),
                "First partition should start at minId");
        assertEquals(maxId, lastPartition.getLong("maxId"),
                "Last partition should end at maxId");
    }

    @Test
    @DisplayName("Partitioner - Should have continuous ID ranges")
    void testContinuousRanges() {
        // Given
        int gridSize = 4;

        // When
        Map<String, ExecutionContext> partitions = partitioner.partition(gridSize);

        // Then - Verify continuity
        for (int i = 0; i < gridSize - 1; i++) {
            ExecutionContext current = partitions.get("partition" + i);
            ExecutionContext next = partitions.get("partition" + (i + 1));

            long currentMax = current.getLong("maxId");
            long nextMin = next.getLong("minId");

            assertEquals(currentMax + 1, nextMin,
                    "Partition " + (i + 1) + " should start right after partition " + i);
        }
    }

    @Test
    @DisplayName("Partitioner - Each partition should have minId and maxId")
    void testPartitionContextContainsRequiredKeys() {
        // Given
        int gridSize = 4;

        // When
        Map<String, ExecutionContext> partitions = partitioner.partition(gridSize);

        // Then
        for (Map.Entry<String, ExecutionContext> entry : partitions.entrySet()) {
            ExecutionContext context = entry.getValue();

            assertTrue(context.containsKey("minId"),
                    entry.getKey() + " should contain minId");
            assertTrue(context.containsKey("maxId"),
                    entry.getKey() + " should contain maxId");

            long minId = context.getLong("minId");
            long maxId = context.getLong("maxId");

            assertTrue(minId <= maxId,
                    entry.getKey() + " minId should be <= maxId");
        }
    }

    // ==================== Edge Case Tests ====================

    @Test
    @DisplayName("Partitioner - Should handle zero gridSize gracefully")
    void testZeroGridSize() {
        // Given
        int gridSize = 0;

        // When
        Map<String, ExecutionContext> partitions = partitioner.partition(gridSize);

        // Then
        assertNotNull(partitions);
        assertEquals(0, partitions.size(), "Zero grid size should produce empty map");
    }

    @Test
    @DisplayName("Partitioner - Should maintain partition order")
    void testPartitionOrder() {
        // Given
        int gridSize = 10;

        // When
        Map<String, ExecutionContext> partitions = partitioner.partition(gridSize);

        // Then - Verify partition ranges are in ascending order
        long previousMax = 0;

        for (int i = 0; i < gridSize; i++) {
            ExecutionContext partition = partitions.get("partition" + i);
            long currentMin = partition.getLong("minId");

            if (i > 0) {
                assertTrue(currentMin > previousMax,
                        "Partition " + i + " should start after partition " + (i - 1));
            }

            previousMax = partition.getLong("maxId");
        }
    }

    // ==================== Partition Size Tests ====================

    @Test
    @DisplayName("Partitioner - Should create roughly equal partition sizes")
    void testEqualPartitionSizes() {
        // Given
        int gridSize = 4;

        // When
        Map<String, ExecutionContext> partitions = partitioner.partition(gridSize);

        // Then - All partitions except last should have same size
        long expectedSize = 2499999; // (10000000 - 1) / 4

        for (int i = 0; i < gridSize - 1; i++) {
            ExecutionContext partition = partitions.get("partition" + i);
            long minId = partition.getLong("minId");
            long maxId = partition.getLong("maxId");
            long actualSize = maxId - minId + 1;

            assertEquals(expectedSize, actualSize,
                    "Partition " + i + " should have size " + expectedSize);
        }
    }

    @Test
    @DisplayName("Partitioner - Last partition may be slightly larger")
    void testLastPartitionSize() {
        // Given
        int gridSize = 4;
        long minId = 1;
        long maxId = 10000000;
        long rangeSize = (maxId - minId) / gridSize;

        // When
        Map<String, ExecutionContext> partitions = partitioner.partition(gridSize);

        // Then - Last partition includes remainder
        ExecutionContext lastPartition = partitions.get("partition3");
        long lastMin = lastPartition.getLong("minId");
        long lastMax = lastPartition.getLong("maxId");
        long lastSize = lastMax - lastMin + 1;

        assertTrue(lastSize >= rangeSize,
                "Last partition should be at least as large as other partitions");
    }

    @Test
    @DisplayName("Partitioner - Total coverage equals full range")
    void testTotalCoverage() {
        // Given
        int gridSize = 4;
        long minId = 1;
        long maxId = 10000000;
        long expectedTotal = maxId - minId + 1;

        // When
        Map<String, ExecutionContext> partitions = partitioner.partition(gridSize);

        // Then - Sum of all partition sizes should equal total range
        long totalCovered = 0;

        for (ExecutionContext partition : partitions.values()) {
            long partitionMin = partition.getLong("minId");
            long partitionMax = partition.getLong("maxId");
            totalCovered += (partitionMax - partitionMin + 1);
        }

        assertEquals(expectedTotal, totalCovered,
                "All partitions together should cover exactly the full range");
    }
}