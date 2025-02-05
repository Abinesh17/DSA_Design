package cache.lru;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class LRUCache {

   public static class Node
    {
        int key;
        String value;
        Node prev, next;
        public Node(int key, String value)
        {
            this.key = key;
            this.value = value;
        }

        @Override
        public String toString() {
           return "key: "+key+" | value: "+value;
        }
    }

    private int maxCapacity;
    ConcurrentHashMap<Integer,Node> idToDataMap;
    Node head, tail;
    private final ReentrantLock lock;

    public LRUCache(int maxCapacity)
    {
        this.maxCapacity = maxCapacity;
        this.idToDataMap = new ConcurrentHashMap<>();
        head = new Node(0, "");
        tail = new Node(0, "");
        head.next = tail;
        tail.prev = head;
        this.lock = new ReentrantLock();
    }

    public String get(int id)
    {
       lock.lock();
        if (!idToDataMap.containsKey(id)) {
            System.out.println(id + " not available in cache");
            return null;
        }

        Node data = idToDataMap.get(id);
        try {

            // consider this as recently used
            // move this data to front
            deleteNode(data);
            addToFront(data);
            return data.value;
        }
        finally
        {
            lock.unlock();
        }
    }

    private void addToFront(Node node)
    {
        Node headNext = head.next;
        head.next = node;
        node.prev = head;
        node.next = headNext;
        headNext.prev = node;
    }

    private void deleteNode(Node node)
    {
        Node prev = node.prev;
        Node next = node.next;
        prev.next = next;
        next.prev = prev;
    }

    public void put(int key, String value)
    {
        lock.lock();
        try {
            if (idToDataMap.containsKey(key)) {
                Node node = idToDataMap.remove(key);
                deleteNode(node);
            }

            Node newNode = new Node(key, value);
            idToDataMap.put(key, newNode);
            addToFront(newNode);

            if (idToDataMap.size() > maxCapacity) {
                Node node = tail.prev;
                deleteNode(node);
                idToDataMap.remove(node.key);
            }
        }
        finally {
            lock.unlock();
        }
    }

    public static void main(String[] args) {
        LRUCache cache = new LRUCache(10);
        for(int i=0;i<12;i++)
        {
            cache.put(i, "data-"+i);
        }
        for(int i=0;i<=12;i++)
        {
            System.out.println(cache.get(i));
        }

    }
}
