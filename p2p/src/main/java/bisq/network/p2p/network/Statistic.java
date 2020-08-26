/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.network.p2p.network;

import bisq.common.UserThread;
import bisq.common.proto.network.NetworkEnvelope;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;

/**
 * Network statistics per connection. As we are also interested in total network statistics
 * we use static properties to get traffic of all connections combined.
 */
@Slf4j
public class Statistic {

    ///////////////////////////////////////////////////////////////////////////////////////////
    // Static
    ///////////////////////////////////////////////////////////////////////////////////////////


    private final static long startTime = System.currentTimeMillis();
    private final static LongProperty totalSentBytes = new SimpleLongProperty(0);
    private final static LongProperty totalReceivedBytes = new SimpleLongProperty(0);
    private final static Map<String, Integer> totalReceivedMessages = new ConcurrentHashMap<>();
    private final static Map<String, Integer> totalSentMessages = new ConcurrentHashMap<>();
    private final static Map<String, Integer> totalReceivedMessagesPerSec = new ConcurrentHashMap<>();
    private final static Map<String, Integer> totalSentMessagesPerSec = new ConcurrentHashMap<>();
    private final static LongProperty numTotalSentMessages = new SimpleLongProperty(0);
    private final static LongProperty numTotalSentMessagesLastSec = new SimpleLongProperty(0);
    private final static DoubleProperty numTotalSentMessagesPerSec = new SimpleDoubleProperty(0);
    private final static LongProperty numTotalReceivedMessages = new SimpleLongProperty(0);
    private final static LongProperty numTotalReceivedMessagesLastSec = new SimpleLongProperty(0);
    private final static DoubleProperty numTotalReceivedMessagesPerSec = new SimpleDoubleProperty(0);

    static {
        UserThread.runPeriodically(() -> {
            numTotalSentMessages.set(totalSentMessages.values().stream().mapToInt(Integer::intValue).sum());
            numTotalSentMessagesLastSec.set(totalSentMessagesPerSec.values().stream().mapToInt(Integer::intValue).sum());
            numTotalReceivedMessages.set(totalReceivedMessages.values().stream().mapToInt(Integer::intValue).sum());
            numTotalReceivedMessagesLastSec.set(totalReceivedMessagesPerSec.values().stream().mapToInt(Integer::intValue).sum());

            long passed = (System.currentTimeMillis() - startTime) / 1000;
            numTotalSentMessagesPerSec.set(((double) numTotalSentMessages.get()) / passed);
            numTotalReceivedMessagesPerSec.set(((double) numTotalReceivedMessages.get()) / passed);
            totalSentMessagesPerSec.clear();
            totalReceivedMessagesPerSec.clear();
        }, 1);

        // We log statistics every minute
        UserThread.runPeriodically(() -> {
            log.error("Network statistics:\n" +
                            "totalSentBytes: {} kb;\n" +
                            "numTotalSentMessages/totalSentMessages: {} / {};\n" +
                            "numTotalSentMessagesLastSec/totalSentMessagesPerSec: {} / {};\n" +
                            "totalReceivedBytes: {} kb" +
                            "numTotalReceivedMessages/totalReceivedMessages: {} / {};\n" +
                            "numTotalReceivedMessagesLastSec/totalReceivedMessagesPerSec: {} / {};",
                    totalSentBytes.get() / 1024d,
                    numTotalSentMessages.get(), totalSentMessages,
                    numTotalSentMessagesLastSec.get(), totalSentMessagesPerSec,
                    totalReceivedBytes.get() / 1024d,
                    numTotalReceivedMessages.get(), totalReceivedMessages,
                    numTotalReceivedMessagesLastSec.get(), totalReceivedMessagesPerSec);
        }, 60);
    }

    public static LongProperty totalSentBytesProperty() {
        return totalSentBytes;
    }

    public static LongProperty totalReceivedBytesProperty() {
        return totalReceivedBytes;
    }

    public static LongProperty numTotalSentMessagesProperty() {
        return numTotalSentMessages;
    }

    public static LongProperty numTotalSentMessagesLastSecProperty() {
        return numTotalSentMessagesLastSec;
    }

    public static DoubleProperty numTotalSentMessagesPerSecProperty() {
        return numTotalSentMessagesPerSec;
    }

    public static LongProperty numTotalReceivedMessagesProperty() {
        return numTotalReceivedMessages;
    }

    public static LongProperty numTotalReceivedMessagesLastSecProperty() {
        return numTotalReceivedMessagesLastSec;
    }

    public static DoubleProperty numTotalReceivedMessagesPerSecProperty() {
        return numTotalReceivedMessagesPerSec;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Instance fields
    ///////////////////////////////////////////////////////////////////////////////////////////

    private final Date creationDate;
    private long lastActivityTimestamp = System.currentTimeMillis();
    private final LongProperty sentBytes = new SimpleLongProperty(0);
    private final LongProperty receivedBytes = new SimpleLongProperty(0);
    private final Map<String, Integer> receivedMessages = new ConcurrentHashMap<>();
    private final Map<String, Integer> sentMessages = new ConcurrentHashMap<>();
    private final IntegerProperty roundTripTime = new SimpleIntegerProperty(0);


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Constructor
    ///////////////////////////////////////////////////////////////////////////////////////////

    Statistic() {
        creationDate = new Date();
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    // Update, increment
    ///////////////////////////////////////////////////////////////////////////////////////////

    void updateLastActivityTimestamp() {
        UserThread.execute(() -> lastActivityTimestamp = System.currentTimeMillis());
    }

    void addSentBytes(int value) {
        UserThread.execute(() -> {
            sentBytes.set(sentBytes.get() + value);
            totalSentBytes.set(totalSentBytes.get() + value);
        });
    }

    void addReceivedBytes(int value) {
        UserThread.execute(() -> {
            receivedBytes.set(receivedBytes.get() + value);
            totalReceivedBytes.set(totalReceivedBytes.get() + value);
        });
    }

    // TODO would need msg inspection to get useful information...
    void addReceivedMessage(NetworkEnvelope networkEnvelope) {
        String messageClassName = networkEnvelope.getClass().getSimpleName();
        int counter = 1;
        if (receivedMessages.containsKey(messageClassName)) {
            counter = receivedMessages.get(messageClassName) + 1;
        }
        receivedMessages.put(messageClassName, counter);

        counter = 1;
        if (totalReceivedMessages.containsKey(messageClassName)) {
            counter = totalReceivedMessages.get(messageClassName) + 1;
        }
        totalReceivedMessages.put(messageClassName, counter);

        counter = 1;
        if (totalReceivedMessagesPerSec.containsKey(messageClassName)) {
            counter = totalReceivedMessagesPerSec.get(messageClassName) + 1;
        }
        totalReceivedMessagesPerSec.put(messageClassName, counter);
    }

    void addSentMessage(NetworkEnvelope networkEnvelope) {
        String messageClassName = networkEnvelope.getClass().getSimpleName();
        int counter = 1;
        if (sentMessages.containsKey(messageClassName)) {
            counter = sentMessages.get(messageClassName) + 1;
        }
        sentMessages.put(messageClassName, counter);

        counter = 1;
        if (totalSentMessages.containsKey(messageClassName)) {
            counter = totalSentMessages.get(messageClassName) + 1;
        }
        totalSentMessages.put(messageClassName, counter);

        counter = 1;
        if (totalSentMessagesPerSec.containsKey(messageClassName)) {
            counter = totalSentMessagesPerSec.get(messageClassName) + 1;
        }
        totalSentMessagesPerSec.put(messageClassName, counter);
    }

    public void setRoundTripTime(int roundTripTime) {
        this.roundTripTime.set(roundTripTime);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    // Getters
    ///////////////////////////////////////////////////////////////////////////////////////////

    public long getLastActivityTimestamp() {
        return lastActivityTimestamp;
    }

    public long getLastActivityAge() {
        return System.currentTimeMillis() - lastActivityTimestamp;
    }

    public long getSentBytes() {
        return sentBytes.get();
    }

    public LongProperty sentBytesProperty() {
        return sentBytes;
    }

    public long getReceivedBytes() {
        return receivedBytes.get();
    }

    public LongProperty receivedBytesProperty() {
        return receivedBytes;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public IntegerProperty roundTripTimeProperty() {
        return roundTripTime;
    }

    @Override
    public String toString() {
        return "Statistic{" +
                "\n     creationDate=" + creationDate +
                ",\n     lastActivityTimestamp=" + lastActivityTimestamp +
                ",\n     sentBytes=" + sentBytes +
                ",\n     receivedBytes=" + receivedBytes +
                ",\n     receivedMessages=" + receivedMessages +
                ",\n     sentMessages=" + sentMessages +
                ",\n     roundTripTime=" + roundTripTime +
                "\n}";
    }
}
