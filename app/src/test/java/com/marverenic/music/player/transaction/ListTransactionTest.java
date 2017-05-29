package com.marverenic.music.player.transaction;

import junit.framework.Assert;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ListTransactionTest {

    OutgoingTransaction<List<String>, RuntimeException> outgoingTransaction;
    IncomingTransaction<List<String>> incomingTransaction;

    private void assertCorrectTransmission(List<String> expected) {
        outgoingTransaction.send(
                token -> incomingTransaction = ListTransaction.receive(token),
                chunk -> incomingTransaction.receive(chunk),
                () -> {
                    List<String> received = incomingTransaction.getData();
                    Assert.assertEquals("Data was not received correctly", expected, received);
                });
    }

    private List<String> generateLongList(int itemCount) {
        List<String> list = new ArrayList<>(itemCount);
        for (int i = 0; i < itemCount; i++) {
            list.add(Integer.toString(i));
        }

        return Collections.unmodifiableList(list);
    }

    @Test
    public void testTransactionSuccessful_noData() {
        List<String> data = Collections.emptyList();
        outgoingTransaction = ListTransaction.send(data);

        assertCorrectTransmission(data);
    }

    @Test
    public void testTransactionSuccessful_smallData() {
        List<String> data = Collections.unmodifiableList(Arrays.asList("Orange", "Apple", "Pear"));
        outgoingTransaction = ListTransaction.send(data);

        assertCorrectTransmission(data);
    }

    @Test
    public void testTransactionSuccessful_oneChunk() {
        List<String> data = generateLongList(TransactionChunk.MAX_ENTRIES);
        outgoingTransaction = ListTransaction.send(data);

        assertCorrectTransmission(data);
    }

    @Test
    public void testTransactionSuccessful_oneAndAHalfChunks() {
        List<String> data = generateLongList(TransactionChunk.MAX_ENTRIES * 3 / 2);
        outgoingTransaction = ListTransaction.send(data);

        assertCorrectTransmission(data);
    }

    @Test
    public void testTransactionSuccessful_manyChunks() {
        List<String> data = generateLongList(TransactionChunk.MAX_ENTRIES * 12);
        outgoingTransaction = ListTransaction.send(data);

        assertCorrectTransmission(data);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTransactionFails_receiveFromDifferentTransaction() {
        // Setup incomingTransaction to read from a garbage Transaction
        ListTransaction.<Void, RuntimeException>send(Collections.emptyList()).send(
                token -> incomingTransaction = ListTransaction.receive(token),
                chunk -> {},
                () -> {});

        List<String> data = Collections.unmodifiableList(Arrays.asList("Orange", "Apple", "Pear"));
        outgoingTransaction = ListTransaction.send(data);

        outgoingTransaction.send(
                token -> {},
                chunk -> incomingTransaction.receive(chunk),
                () -> incomingTransaction.getData());
    }

    @Test(expected = IllegalStateException.class)
    public void testTransactionFails_prematureRead() {
        List<String> data = Collections.unmodifiableList(Arrays.asList("Orange", "Apple", "Pear"));
        outgoingTransaction = ListTransaction.send(data);

        outgoingTransaction.send(
                token -> incomingTransaction = ListTransaction.receive(token),
                chunk -> {
                    incomingTransaction.getData();
                    incomingTransaction.receive(chunk);
                },
                () -> {});
    }

}
