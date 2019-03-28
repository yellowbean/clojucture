package clojucture;

import tech.tablesaw.api.DateColumn;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.joining.DataFrameJoiner;

import java.time.LocalDate;
import java.util.Arrays;

enum Direction
{
    FORWARD,BACKWARD;
}



public class DoubleFlow extends Table {


    public DoubleFlow(String name, LocalDate[] d, double[] o){
        super(name);
        DateColumn dts = DateColumn.create("DATES", d);
        DoubleColumn sc = DoubleColumn.create("Double", o);
        this.addColumns(dts, sc);
    }

    public DoubleFlow(String name, LocalDate[] d){
        super(name);
        DateColumn dts = DateColumn.create("DATES", d);
        this.addColumns(dts);
    }


    public DoubleFlow align(String name, LocalDate[] d){

        DoubleFlow targetFlow = new DoubleFlow(name, d);
        Double[] targetValue = new Double[d.length];
        Arrays.fill(targetValue, Double.NaN);
        int cmp_index = d.length - 1;

        for(int i = this.column(0).size()-1; i>=0; i--){

            LocalDate cmpDate = ((DateColumn)this.column(0)).get(i);
            Double valSet = ((DoubleColumn)this.column(1)).get(i);

            while( cmp_index>=0 ) {
                if (d[cmp_index].isAfter(cmpDate)){
                    targetValue[cmp_index] = valSet;
                    cmp_index--;
                }else{
                    break;
                }
            }
        }

        DoubleColumn dv = DoubleColumn.create("Double", targetValue);
        targetFlow.addColumns(dv);
        return targetFlow;
    }

}
