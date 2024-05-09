package net.silkmc.silk.compose.internal

internal object MapIdGenerator {
    // for now, use the top 100000 ids for fake maps
    private var currentMaxId = Int.MAX_VALUE - 100_000

    // these are IDs which have been used before for fake maps
    // and are now available for reuse
    private val availableOldIds = ArrayList<Int>()

    fun nextId(): Int {
        return synchronized(this) {
            availableOldIds.removeFirstOrNull() ?: (++currentMaxId)
        }
    }

    fun makeOldIdsAvailable(ids: Collection<Int>) {
        synchronized(this) {
            availableOldIds.addAll(ids)
        }
    }
}
