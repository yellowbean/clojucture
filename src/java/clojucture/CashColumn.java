package clojucture;

import tech.tablesaw.api.DoubleColumn;
public class CashColumn extends DoubleColumn{

    public CashColumn(String name, Double[] v){
        super(name);
        DoubleColumn cc = DoubleColumn.create(name, v);

    }
    /*
    public Add(CashColumn x){

        this.add(x); //
                     //

        return (CashColumn)this;
    }

     */



}
