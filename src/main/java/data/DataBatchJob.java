package data;

import data.dto.CategorySalesdto;
import data.entities.OrderItem;
import data.entities.Product;
import org.apache.flink.api.common.functions.JoinFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.api.common.io.OutputFormat;
import org.apache.flink.api.common.operators.Order;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.operators.DataSource;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.tuple.Tuple6;
import org.apache.flink.configuration.Configuration;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class DataBatchJob {

	public static void main(String[] args) throws Exception {

		final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

		DataSource<OrderItem> orderItems = env.readCsvFile("/home/stilinski/IdeaProjects/data/dataset/order_items.csv")
				.ignoreFirstLine().pojoType(OrderItem.class, "OrderItemID", "OrderID", "ProductID", "Quantity", "PricePerUnit");

		DataSource<Product> products = env.readCsvFile("/home/stilinski/IdeaProjects/data/dataset/products.csv")
				.ignoreFirstLine().pojoType(Product.class, "ProductID", "Name", "Description", "Price","Category");

		//join the datasets on the product Id
		DataSet<Tuple6<String, String, Float, Integer, Float, String>> joined = orderItems
				.join(products)
				.where("ProductID")
				.equalTo("ProductID")
				.with((JoinFunction<OrderItem, Product, Tuple6<String, String, Float, Integer, Float, String>>) (first, second)
						-> new Tuple6<>(
						second.ProductID.toString(),
						second.Name,
						first.PricePerUnit,
						first.Quantity,
						first.PricePerUnit * first.Quantity,
						second.Category
				))
				.returns(TypeInformation.of(new TypeHint<Tuple6<String, String, Float, Integer, Float, String>>() {
				}));

		//group  by category to get total sales and count
		DataSet<CategorySalesdto> categorySales = joined
				.map((MapFunction<Tuple6<String, String, Float, Integer, Float, String>, CategorySalesdto>) record
						-> new CategorySalesdto(record.f5, record.f4, 1)).returns(CategorySalesdto.class).groupBy("Category")
				.reduce((ReduceFunction<CategorySalesdto>)(value1,value2) ->
				new CategorySalesdto(value1.getCategory(),value1.getTotalSales()+value2.getTotalSales(),value1.getCount()+value2.getCount()));

		categorySales.print();

		//
		categorySales.output(new OutputFormat<CategorySalesdto>() {

			private transient BufferedWriter writer;
			@Override
			public void configure(Configuration configuration) {

			}
			@Override
			public void open(int taskNumber,int numTasks) throws IOException{
				File outputfile = new File("/home/stilinski/IdeaProjects/data/dataset/new-output.csv");
				this.writer=new BufferedWriter(new FileWriter(outputfile, true));

			}

			@Override
			public void writeRecord(CategorySalesdto CategorySalesdto) throws IOException {
				writer.write(CategorySalesdto.getCategory()
						+ "," + CategorySalesdto.getTotalSales()
						+ "," + CategorySalesdto.getCount());
				writer.newLine();
				writer.newLine();



			}

			@Override
			public void close() throws IOException {
				writer.close();

			}
		})


		//sort bu total sales in descending order
		categorySales.sortPartition("totalSales", Order.DESCENDING).print();



		// Execute program, beginning computation.
		env.execute("Flink Java API Skeleton");
	}
}
