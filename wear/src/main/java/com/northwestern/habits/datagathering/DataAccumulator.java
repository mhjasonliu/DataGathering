package com.northwestern.habits.datagathering;

import android.hardware.SensorEvent;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;

/**
 * Created by William on 1/29/2017.
 */
public class DataAccumulator {
    private static final String TAG = "DataAccumulator";
    private LinkedList<CustomSensorEvent> events = new LinkedList<>();
    /**
     * Interval for which the energy is calculated (in milliseconds)
     * 60000 corresponds to a minute
     */
    private final long TIME_INTERVAL = 60000/6;

    public DataAccumulator() {}

    public DataAccumulator(byte[] bytes, int length) {

    }

    /**
     * Adds the given event to the list
     * If TIME_INTERVAL  has passed since first element, calculates the sum of squared differences
     * of accelerometer values and returns it as an energy indicator.
     * Algorithm for calculating mean square error was taken from
     * <a href="http://stackoverflow.com/questions/26532832/calculating-sample-variance-in-java-but-is-giving-the-wrong-answer-when-inserti">this</a>
     * discussion.
     *
     * @param sensorEvent The accelerometer event to be added
     * @return null if a minute has not passed,
     * energy reading with the sum squared variance if a minute has passed
     */
    public boolean addEvent(SensorEvent sensorEvent) {
        CustomSensorEvent e = new CustomSensorEvent(sensorEvent);
        events.addLast(e);

        if ((e.timestamp - events.getFirst().timestamp) < TIME_INTERVAL) { // Interval has not passed
            return false;
        } else {
            return true;
        }
    }

    public byte[] getAsBytes() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(events.size()*CustomSensorEvent.BYTELENGHT);

        byte[] tmp = null;
        boolean first = true;
        for (CustomSensorEvent event :
                events) {
            if (first) {
                Log.v(TAG, "First float: " + Float.toString(event.values[0]));
                first = false;
            }
            try {
                tmp = event.toByteArray();
                if (tmp != null) bytes.write(tmp);
            } catch (IOException e) {
                // Failed to turn event into array, just don't add it
                e.printStackTrace();
            }
        }

        return bytes.toByteArray();
    }

    /**
     * Class with relevant fields of a SensorEvent
     */
    private class CustomSensorEvent {
        /**
         * Timestamp in milliseconds of instantiation
         */
        public long timestamp;
        /**
         * Data payload of the SensorEvent
         */
        public float[] values;

        /**
         * Default constructor. Should never be called.
         */
        private CustomSensorEvent() {
            throw new IllegalArgumentException("This class should only be instantiated " +
                    "with a SensorEvent");
        }

        /**
         * Instantiates a CustomSensorEvent by copying the fields from e
         *
         * @param e SensorEvent (Accelerometer) to be copied
         */
        public CustomSensorEvent(SensorEvent e) {
            timestamp = System.currentTimeMillis();
            values = new float[]{e.values[0], e.values[1], e.values[2]};
        }

        public byte[] toByteArray() throws IOException {
            ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream(BYTELENGHT);

            byteBuffer.write(float2ByteArray(values[0]));
            byteBuffer.write(float2ByteArray(values[1]));
            byteBuffer.write(float2ByteArray(values[2]));
            byteBuffer.write(long2ByteArray(timestamp));

            return byteBuffer.toByteArray();
        }


        public static final int BYTELENGHT = 8*3+4;

        public byte [] long2ByteArray (long value) {
            return ByteBuffer.allocate(8).putLong(value).array();
        }

        public byte [] float2ByteArray (float value)
        {
            return ByteBuffer.allocate(4).putFloat(value).array();
        }
    }
}
