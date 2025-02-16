package me.huan.aqs;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

/**
 * MyLock
 *
 * @author hhuzzz
 * @version 2025/02/16 17:03
 */
public class MyLock {
    Thread owner = null;

    AtomicReference<Node> head = new AtomicReference<Node>(new Node());
    AtomicReference<Node> tail = new AtomicReference<Node>(head.get());

    AtomicBoolean flag = new AtomicBoolean(false);

    void lock() {
        // 先判定能否直接拿到锁
        if (flag.compareAndSet(false, true)) {
            System.out.println(Thread.currentThread().getName() + "直接拿到锁");
            owner = Thread.currentThread();
            return;
        }
        Node current = new Node();
        current.thread = Thread.currentThread();
        // 没拿到锁，自旋加入AQS队列尾部
        while (true) {
            Node currentTail = tail.get();
            if (tail.compareAndSet(currentTail, current)) {
                System.out.println(Thread.currentThread().getName() + "加入到了链表尾");
                current.prev = currentTail;
                currentTail.next = current;
                break;
            }
        }

        // 阻塞，如果被唤醒，则退出循环
        while (true) {
            if (current.prev == head.get() && flag.compareAndSet(false, true)) {
                owner = Thread.currentThread();
                head.set(current);
                current.prev.next = null;
                current.prev = null;
                System.out.println(Thread.currentThread().getName() + "被唤醒之后，拿到锁");
                return;
            }
            LockSupport.park();
        }
    }

    void unlock() {
        if (Thread.currentThread() != owner) {
            throw new IllegalStateException("当前线程没有锁，不能解锁");
        }
        Node headNode = head.get();
        Node next = headNode.next;
        flag.set(false);
        if (next != null) {
            System.out.println(Thread.currentThread().getName() + "唤醒了" + next.thread.getName());
            LockSupport.unpark(next.thread);
        }
    }

    static class Node {
        Node next;
        Node prev;
        Thread thread;

        Node() {

        }
    }
}