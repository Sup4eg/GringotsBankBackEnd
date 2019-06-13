import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public class Exchange extends Storage {

    private Connection dbConnection = null;
    private Statement statement = null;

    {
        try {
            dbConnection = ConnectionDB.getDBConnection();
            statement = dbConnection.createStatement();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Exchange exchange = new Exchange();
//        exchange.insertDataToExchangeCurrencySQL("rub", 1300280, 0.00435, 0.00439, 0, 0, 0);
//        exchange.insertDataToExchangeCurrencySQL("usd", 763800, 0.01512, 0.01712, 0, 0, 0);
//        exchange.insertDataToExchangeCurrencySQL("gbp", 940000, 0.20312, 0.22712, 0, 0, 0);
//        exchange.insertDataToExchangeCurrencySQL("mag", 3750000, 4.93012, 4.93912, 0, 0, 0); //Относитльно фунта стерлингов
//        exchange.getExchangeCurrency();
        exchange.changeGalleonsMoney("usd", 10234);
    }

    //Метод для вставки данных в таблицу EXCHANGE_CURRENCY

    private Boolean insertDataToExchangeCurrencySQL(String currency, int balance_beginning, double purchase_rate, double selling_rate, int remainder, int bought, int sold) {
        String insertDataToExchangeCurrencySQL = "INSERT INTO EXCHANGE_CURRENCY"
                + "(CURRENCY, BALANCE_BEGINNING, PURCHASE_RATE, SELLING_RATE, REMAINDER, BOUGHT, SOLD) " + "VALUES" +
                String.format("('%s',%d,%s,%s,%d,%d,%d)", currency.toUpperCase(), balance_beginning, String.valueOf(purchase_rate), String.valueOf(selling_rate), remainder, bought, sold);

        try {
            statement.executeQuery(insertDataToExchangeCurrencySQL);
        } catch (SQLException e) {
            System.out.println("Currency data wasn't inserted");
            System.out.println(e.getMessage());
            return false;
        }
        System.out.println("Currency data was inserted");
        return true;
    }


    //Метод делает обмен галлионов в любую валюту

    private Boolean changeGalleonsMoney(String to_money, int galleons) {

        // увеличить покупку и продажу соответствующей валюты

        double mag_new_sold = getMoneyParameter("mag", "sold").getArgument().doubleValue() + galleons;
        double mag_bought = getMoneyParameter("mag", "bought").getArgument().doubleValue();

        double purchase_rate = getMoneyParameter(to_money, "purchase_rate").getArgument().doubleValue();
        double exchange_money = purchase_rate * galleons;

        double to_money_new_bought = getMoneyParameter(to_money, "bought").getArgument().doubleValue() + exchange_money;
        double to_money_sold = getMoneyParameter(to_money, "sold").getArgument().doubleValue();

        updateTable("exchange_currency", "sold", new MultiArgument<Double>(mag_new_sold), "currency", new MultiArgument<String>("mag"));
        updateTable("exchange_currency", "bought", new MultiArgument<Double>(to_money_new_bought), "currency", new MultiArgument<String>(to_money));

        //обновить остаток

        double mag_balance_beginning = getMoneyParameter("mag", "balance_beginning").getArgument().doubleValue();
        double to_money_balance_beginning = getMoneyParameter(to_money, "balance_beginning").getArgument().doubleValue();

        double new_mag_remainder = mag_balance_beginning + mag_bought - mag_new_sold;
        double new_to_money_remainder = to_money_balance_beginning + to_money_new_bought - to_money_sold;

        updateTable("exchange_currency", "remainder", new MultiArgument<Double>(new_mag_remainder), "currency", new MultiArgument<String>("mag"));
        updateTable("exchange_currency", "remainder", new MultiArgument<Double>(new_to_money_remainder), "currency", new MultiArgument<String>(to_money));



        return true;
    }


    //Метод возвращает все строки из таблицы EXCHANGE_CURRENCY

    private Map getExchangeCurrency() {
        String selectAllFromExchangeCurrencySQL = "SELECT * FROM EXCHANGE_CURRENCY";
        Map <String, String[]> exchange_map = new HashMap<String, String[]>();
        try {
            ResultSet rs = statement.executeQuery(selectAllFromExchangeCurrencySQL);
            while (rs.next()) {
                String currency = rs.getString("CURRENCY");
                String balance_beginning = rs.getString("BALANCE_BEGINNING");
                String purchase_rate = rs.getString("PURCHASE_RATE");
                String selling_rate = rs.getString("SELLING_RATE");
                String remainder = rs.getString("REMAINDER");
                String bought = rs.getString("BOUGHT");
                String sold = rs.getString("SOLD");
                exchange_map.put(currency, new String[]{balance_beginning, purchase_rate, selling_rate, remainder, bought, sold});
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return exchange_map;
    }

// Класс generic для извлечения double и int инф. из строк таблицы

    private NumberArguments getMoneyParameter(String currency, String column) {
        String selectFromExchangeCurrentSQL = String.format("SELECT %s FROM EXCHANGE_TABLE WHERE CURRENCY = '%s'", column.toUpperCase(), currency.toUpperCase());
        NumberArguments parameter = null;
        try {
            ResultSet rs = statement.executeQuery(selectFromExchangeCurrentSQL);
            while (rs.next()) {
                if (!rs.getString(column).contains(".")) {
                    parameter = new NumberArguments<Integer>(Integer.parseInt(rs.getString(column)));
                } else {
                    parameter = new NumberArguments<Double>(Double.parseDouble(rs.getString(column)));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return parameter;
    }

//    Метод получает числовой параметр из таблцы EXCHANGE_TABLE

    private class NumberArguments<T extends Number> {
        private T argument;
        NumberArguments(T argument) {
            this.argument = argument;
        }
        public T getArgument() {
            return argument;
        }
    }
}
