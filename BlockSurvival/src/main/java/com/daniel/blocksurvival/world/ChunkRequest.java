package com.daniel.blocksurvival.world;

/*
 * A lightweight request for one chunk coordinate.
 *
 * Smaller priority values are processed first.
 */
public record ChunkRequest(
        int chunkX,
        int chunkY,
        int chunkZ,
        int priority,
        long sequence
) implements Comparable<ChunkRequest> {

    @Override
    public int compareTo(
            ChunkRequest other
    ) {
        int priorityComparison =
                Integer.compare(
                        priority,
                        other.priority
                );

        if (priorityComparison != 0) {
            return priorityComparison;
        }

        /*
         * Preserve queue order when two requests have
         * the same priority.
         */
        return Long.compare(
                sequence,
                other.sequence
        );
    }
}