package de.drick.wtf_osd_player.tools

class LRUCache<K, V>(private val capacity: Int) {
    private val cache = LinkedHashMap<K, V>(capacity, 0.75F)

    operator fun get(key: K): V? {
        if (!cache.containsKey(key)) return null
        return cache.remove(key)?.also {
            cache[key] = it // move to end (last recently used)
        }
    }
    operator fun set(key: K, value: V) = put(key, value)
    fun put(key: K, value: V) {
        if (cache.containsKey(key)) {
            cache.remove(key)
        } else if (cache.size == capacity) {
            cache.remove(cache.keys.first()) // Remove least recently used
        }
        cache[key] = value
    }

    fun remove(key: K) = cache.remove(key)
    fun clear() = cache.clear()
    val size get() = cache.size
    val keys get() = cache.keys
    val values get() = cache.values
}
