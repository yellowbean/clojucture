package clojucture;

import tech.tablesaw.api.DateColumn;
import tech.tablesaw.api.Table;

import java.time.LocalDate;
import java.util.Arrays;

public class Cashflow extends Table {

    public Cashflow(String name){
        super(name);
    }

    public Cashflow(String name, LocalDate[] d){
        super(name);
        DateColumn dts = DateColumn.create("DATES", d);
        this.addColumns(dts);
    }

    public Cashflow aggregateByInterval(String name, LocalDate[] d){

        Cashflow AggregatedCashflow = new Cashflow(name);
        LocalDate[] endDates = Arrays.copyOfRange(d, 1, d.length);
        LocalDate[] startDates = Arrays.copyOfRange(d, 0, d.length-1);

        DateColumn startDate = DateColumn.create("Start Date", startDates);
        DateColumn endDate = DateColumn.create("End Date", endDates);


        return AggregatedCashflow;
    }
}
