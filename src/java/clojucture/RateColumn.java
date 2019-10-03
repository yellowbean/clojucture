package clojucture;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import org.apache.commons.lang3.ArrayUtils;
import tech.tablesaw.api.DoubleColumn;


public class RateColumn extends DoubleColumn  {
    public RateColumn(String name, Double[] v) {
        super(name, new DoubleArrayList(ArrayUtils.toPrimitive(v)));
    }
}


