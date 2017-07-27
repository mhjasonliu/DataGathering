/**
 * Created by andrey on 7/27/17.
 */
import android.os.SystemClock;
import android.util.Log;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import com.northwestern.habits.datagathering.DataAccumulator;

public class DataAccumulatorTest {

    @Test
    public void checkString() {
        DataAccumulator buf = new DataAccumulator("Accel", 192);
        Map<String, Object> dataPoint = new HashMap<>();
        long timestamp = 1499999923;
        dataPoint.put("Time", timestamp);
        dataPoint.put("accX", 0.1);
        dataPoint.put("accY", 0.4);
        dataPoint.put("accZ", 0.9);

        buf.putDataPoint(dataPoint,timestamp);

        String out = buf.toString();
        String header = buf.getHeader();
        assertTrue(out.equals("1499999923,0.1,0.4,0.9\n"));

        assertTrue(header.equals("Time,accX,accY,accZ\n"));

    }

}
