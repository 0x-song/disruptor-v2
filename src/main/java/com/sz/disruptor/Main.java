package com.sz.disruptor;

import com.sz.disruptor.barrier.SequenceBarrier;
import com.sz.disruptor.buffer.RingBuffer;
import com.sz.disruptor.event.OrderEventHandler;
import com.sz.disruptor.model.OrderEventModel;
import com.sz.disruptor.model.factory.OrderEventModelFactory;
import com.sz.disruptor.processor.BatchEventProcessor;
import com.sz.disruptor.sequence.Sequence;
import com.sz.disruptor.strategy.BlockingWaitStrategy;

/**
 * @Author
 * @Date 2024-12-08 21:00
 * @Version 1.0
 *///TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.

/**
 * A,B------> C----->E
 *    ------->D------>F,G
 */
public class Main {
    public static void main(String[] args) {
        int ringBufferSize = 16;

        RingBuffer<OrderEventModel> ringBuffer = RingBuffer.createSingleProducer(new OrderEventModelFactory(), ringBufferSize, new BlockingWaitStrategy());

        //最上游的序列屏障，只维护了生产者的序列信息
        SequenceBarrier sequenceBarrier = ringBuffer.newBarrier();

        //消费者A
        BatchEventProcessor<OrderEventModel> eventProcessorA = new BatchEventProcessor<>(ringBuffer, new OrderEventHandler("consumerA"), sequenceBarrier);

        Sequence consumerSequenceA = eventProcessorA.getCurrentConsumerSequence();

        ringBuffer.addGatingConsumerSequence(consumerSequenceA);

        //创建消费者B
        BatchEventProcessor<OrderEventModel> eventProcessorB = new BatchEventProcessor<>(ringBuffer, new OrderEventHandler("consumerB"), sequenceBarrier);

        Sequence consumerSequenceB = eventProcessorB.getCurrentConsumerSequence();

        ringBuffer.addGatingConsumerSequence(consumerSequenceB);

        //消费者C依赖于消费者A、B，利用A、B序列号来创建序列屏障
        SequenceBarrier sequenceBarrierC = ringBuffer.newBarrier(consumerSequenceA, consumerSequenceB);
        BatchEventProcessor<OrderEventModel> eventProcessorC = new BatchEventProcessor<>(ringBuffer, new OrderEventHandler("consumerC"), sequenceBarrierC);

        Sequence consumerSequenceC = eventProcessorC.getCurrentConsumerSequence();
        ringBuffer.addGatingConsumerSequence(consumerSequenceC);

        //消费者E依赖于消费者C
        SequenceBarrier mySequenceBarrierE = ringBuffer.newBarrier(consumerSequenceC);
        // 基于序列屏障，创建消费者E
        BatchEventProcessor<OrderEventModel> eventProcessorE =
                new BatchEventProcessor<>(ringBuffer, new OrderEventHandler("consumerE"), mySequenceBarrierE);
        Sequence consumeSequenceE = eventProcessorE.getCurrentConsumerSequence();
        // RingBuffer监听消费者E的序列
        ringBuffer.addGatingConsumerSequence(consumeSequenceE);

        //消费者D依赖于A、B，通过A、B来创建序列屏障
        SequenceBarrier mySequenceBarrierD = ringBuffer.newBarrier(consumerSequenceA,consumerSequenceB);
        // 基于序列屏障，创建消费者D
        BatchEventProcessor<OrderEventModel> eventProcessorD =
                new BatchEventProcessor<>(ringBuffer, new OrderEventHandler("consumerD"), mySequenceBarrierD);
        Sequence consumeSequenceD = eventProcessorD.getCurrentConsumerSequence();
        // RingBuffer监听消费者D的序列
        ringBuffer.addGatingConsumerSequence(consumeSequenceD);

        //消费者F依赖于D
        SequenceBarrier mySequenceBarrierF = ringBuffer.newBarrier(consumeSequenceD);
        // 基于序列屏障，创建消费者F
        BatchEventProcessor<OrderEventModel> eventProcessorF =
                new BatchEventProcessor<>(ringBuffer, new OrderEventHandler("consumerF"), mySequenceBarrierF);
        Sequence consumeSequenceF = eventProcessorF.getCurrentConsumerSequence();
        // RingBuffer监听消费者F的序列
        ringBuffer.addGatingConsumerSequence(consumeSequenceF);


        //消费者G依赖于D
        SequenceBarrier mySequenceBarrierG = ringBuffer.newBarrier(consumeSequenceD);
        // 基于序列屏障，创建消费者G
        BatchEventProcessor<OrderEventModel> eventProcessorG =
                new BatchEventProcessor<>(ringBuffer, new OrderEventHandler("consumerG"), mySequenceBarrierG);
        Sequence consumeSequenceG = eventProcessorG.getCurrentConsumerSequence();
        // RingBuffer监听消费者G的序列
        ringBuffer.addGatingConsumerSequence(consumeSequenceG);

        // 启动消费者线程
        new Thread(eventProcessorA).start();
        new Thread(eventProcessorB).start();
        new Thread(eventProcessorC).start();
        new Thread(eventProcessorD).start();
        new Thread(eventProcessorE).start();
        new Thread(eventProcessorF).start();
        new Thread(eventProcessorG).start();


        // 生产者发布100个事件
        for(int i=0; i<100; i++) {
            long nextIndex = ringBuffer.next();
            OrderEventModel orderEvent = ringBuffer.get(nextIndex);
            orderEvent.setMessage("message-"+i);
            orderEvent.setPrice(i * 10);
            System.out.println("生产者发布事件：" + orderEvent);
            ringBuffer.publish(nextIndex);
        }
    }
}