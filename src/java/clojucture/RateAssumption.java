package clojucture;
import org.apache.commons.lang3.ArrayUtils;
import tech.tablesaw.api.DateColumn;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.Table;
import java.time.LocalDate;
import java.time.Period;
import java.util.*;

public class RateAssumption extends Table{

    public RateAssumption(Table t){
        super(t.name());
        this.addColumns(t.columnArray());
    }

    public RateAssumption(String name, LocalDate[] s, LocalDate[] e, Double[] o){

       super(name);

       DateColumn sd = DateColumn.create("Start",s);
       DateColumn ed = DateColumn.create("End",e);
       DoubleColumn v =DoubleColumn.create("Rate",o); // day rate
       this.addColumns(sd,ed,v);
    }

    public RateAssumption(String name , LocalDate[] d, Double[] o){
       super(name);
       LocalDate[] sda = new LocalDate[d.length - 1];
       LocalDate[] eda = new LocalDate[d.length - 1];

       if((d.length - o.length) != 1)
           throw new IllegalArgumentException(
                   "dates array size should be one more than rate array: current date array size:"
                    + d.length + " rate array size:" + o.length );


       for(int i=0;i<d.length - 1;i++){
          sda[i] = d[i] ;
          eda[i] = d[i+1];
       }

       DateColumn sd = DateColumn.create("Start",sda);
       DateColumn ed = DateColumn.create("End",eda);
       DoubleColumn v = DoubleColumn.create("Rate", o);
       this.addColumns(sd,ed,v);

    }

    public double rateAt(LocalDate d){
        for(int i=0;i<this.rowCount();i++){
            LocalDate s = (LocalDate)this.get(i,0); // start date
            LocalDate e = (LocalDate)this.get(i,1); // end date
            Double v = (Double)this.get(i,2); // value

            if (d.isAfter(s) && d.isBefore(e)){
                return v;
            }
            if (d.equals(s) ){
                return v;
            }
        }

        return Double.NaN;
    }

    public RateAssumption project(LocalDate[] d){
        ArrayList<LocalDate> bps = new ArrayList<>(Arrays.asList(d));
        ArrayList<LocalDate> start_series = new ArrayList<>(((DateColumn)this.column("Start")).asList());
        start_series.remove(0);

        bps.addAll(start_series);
        Collections.sort(bps);

        ArrayList<Double> ratesAtPoints = new ArrayList<Double>();

        for (LocalDate td : bps){
            Double pv = this.rateAt(td);
            //System.out.println("Rate at : Date " + td);
            ratesAtPoints.add(pv);
        }

        Double[] dda = new Double[ratesAtPoints.size()];
        LocalDate[] ddd = new LocalDate[bps.size()];
        Double[] v = ArrayUtils.subarray(ratesAtPoints.toArray(dda),0,dda.length -1);
        return new RateAssumption("Projected",bps.toArray(ddd),v);
    }

    public ArrayList<Double> apply(LocalDate[] d){
        RateAssumption projected_assump = this.project(d);
        // ArrayList<LocalDate> d_list = new ArrayList<>(Arrays.asList(d));
        ArrayList<Double> v_list = new ArrayList<>(d.length);

        // Iterator<LocalDate> d_list_itr = d_list.iterator();


        // only one projected row
        if (projected_assump.rowCount()==1){
            v_list.add((Double)projected_assump.get(0,2));
            return v_list;
        }


        // more than one projected rows
        // LocalDate current_date = d_list_itr.next(); // first date of observations
        for(int i = 0; i< projected_assump.rowCount(); i++){
            LocalDate sDate = (LocalDate)projected_assump.get(i,0);
            LocalDate eDate = (LocalDate)projected_assump.get(i,1);
            Double v = (Double)projected_assump.get(i,2);

            long daysBetween  = Period.between(sDate,eDate).getDays();
            Double period_rate = 1 - Math.pow(1 - v, daysBetween);
            v_list.add(period_rate);
            /*
            if(sDate.compareTo(current_date) == 0 ){
                v_list.add(accum_rate);
                accum_rate = 0.0;
                if (d_list_itr.hasNext())
                    current_date = d_list_itr.next();
            }
            */
        }
        return v_list;
    }

}
