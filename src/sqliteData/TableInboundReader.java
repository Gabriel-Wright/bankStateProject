package sqliteData;

import transactions.Transaction;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

public class TableInboundReader extends TableInbound {

	private TableCategoryReader tCR;

	/*
	 * ============CONSTRUCTORS=================
	 */

	public TableInboundReader(TableInbound tI, TableCategoryReader tCR) {
		super(tI.getDB());
		this.tCR = tCR;
	}

	/*
	 * ============GETTERS=====================
	 */

	public TableCategory getTC() {
		return tCR;
	}

	/*
	 * ============METHODS===================
	 */

	// Provides an array of all inbound transactions, within startDate and endDate
	// for given category
	public Transaction[] extractInboundTransactionArray(String startDate, String endDate, String category) {
		// If category does not exist in Category table -- return null
		if (!tCR.checkCategoryOption(category)) {
			System.out.println("No categories of that type found");
			logger.info("No categories of type:"+category+" found.");
			return null;
		}
		String queryString = "SELECT * FROM " + getTableName() + " WHERE strftime('%Y-%m-%d'," + "Date) BETWEEN '"
				+ startDate + "' AND '" + endDate + "' AND Category = '" + category + "'" + "ORDER BY 'Date' ASC;";
		ArrayList<Transaction> transactions = new ArrayList<>();

		try (Connection connection = DriverManager.getConnection(getDB().getUrl())) {
			Statement stmt = connection.createStatement();

			ResultSet rs = stmt.executeQuery(queryString);

			while (rs.next()) {
				Transaction transaction = new Transaction();
				transaction.setDate(rs.getString("Date"));
				transaction.setProcessedDescription(rs.getString("ProcessDescription"));
				transaction.setPaidIn(rs.getDouble("Paid_In"));
				transaction.setBalance(rs.getDouble("Balance"));

				transactions.add(transaction);
			}
		} catch (SQLException e) {
			logger.error("Failed to read Inbound transactions from Table" + getTableName() + " for dates between "
					+ startDate + " and " + endDate + ", for category:" + category);
		}
		Transaction[] transArr = transactions.toArray(new Transaction[transactions.size()]);
		logger.info("Array of Inbound transactions read from Table"+ getTableName() + " for dates between "
					+ startDate + " and " + endDate + ", for category:" + category);
		return transArr;
	}
	
	// Provides a Map of all Inbound transaction within the date range and for the desired category.
	// This map sorts the transactions into the months within which they took place.
	public Map<Integer, List<Transaction>> extractInboundTransactionMap(String startDate, String endDate, String category) {
		//If no entires of provided category.
		if(!tCR.checkCategoryOption(category)) {
			System.out.println("No categories of that type found");
			logger.info("No categories of type:"+category+" found.");
			return null;
		}
		String queryString = "SELECT * FROM "+getTableName()
				+" WHERE strftime('%Y-%m-%d',"
				+"Date) BETWEEN '"+startDate+ "' AND '"+endDate
				+"' AND Category = '"+category+"'"
						+ "ORDER BY 'Date' ASC;";
		
		//Save different lists of transactions into a transactionsByMonth map
		Map<Integer, List<Transaction>> transactionsByMonth = new HashMap<>();
		
		try(Connection connection = DriverManager.getConnection(getDB().getUrl())){
			Statement stmt = connection.createStatement();
			
			ResultSet rs = stmt.executeQuery(queryString);
			
			while(rs.next()) {
				//Retrieve transaction data from result set
				Transaction transaction = new Transaction();
				transaction.setDate(rs.getString("Date"));
				transaction.setProcessedDescription(rs.getString("ProcessDescription"));
				transaction.setPaidIn(rs.getDouble("Paid_In"));
				transaction.setBalance(rs.getDouble("Balance"));
				
				//Parse month from date.
				String[] dateParts = transaction.getDate().split("-");
				int month = Integer.parseInt(dateParts[1]);
				
				//If month already exists in map -> add it
				if(transactionsByMonth.containsKey(month)) {
					//Add transaction to existing list
					transactionsByMonth.get(month).add(transaction);
				} else {
					//Create a new list for the month and add the relevant transaction to it
					List<Transaction> monthTransactions = new ArrayList<>();
					monthTransactions.add(transaction);
					transactionsByMonth.put(month, monthTransactions);
				}
			}
		} catch (SQLException e) {
			logger.error("Failed to read Inbound transactions into a Map from Table" + getTableName() + " for dates between "
					+ startDate + " and " + endDate + ", for category:" + category);		
			}
		logger.info("Map of transactions, sorted per month, has been read from Table"+ getTableName() + " for dates between "
				+ startDate + " and " + endDate + ", for category:" + category);
		return transactionsByMonth;
	}
}



