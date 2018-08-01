/**
 * Class representing a single line of input data.
 *
 * Data format of input file must be MODEL;MODELYEAR;INVOICEDATE;REBATE;SALES
 *  with RABATT being the rebate of the day in percent (not in any currency!!)
 * The data has to be ordered by INVOICEDATE, MODEL, MODELYEAR
 *
 * @author Leo Knoll
 */
public class RebateData {

    private int name;
    private int year;
    private long date;
    private double rebate;
    private int sales;


    public RebateData() {

    }

    public RebateData(int name, int year, long date, double rebate, int sales) {
        this.name = name;
        this.year = year;
        this.date = date;
        this.rebate = rebate;
        this.sales = sales;
    }


    // getters and setters

    public int getName() {
        return name;
    }

    public void setName(int name) {
        this.name = name;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public double getRebate() {
        return rebate;
    }

    public void setRebate(double rebate) {
        this.rebate = rebate;
    }

    public int getSales() {
        return sales;
    }

    public void setSales(int sales) {
        this.sales = sales;
    }

    @Override
    public String toString() {
        return "Name: " + name + ", Year: " + year + ", Date: " + date
            + ", Rebate: " + rebate + ", Sales: " + sales;
    }
}
