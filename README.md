Change the branch to access the files

## Sales Analysis using Flink
In this project, we will be performing a sales analysis using Apache Flink.

Apache Flink is a framework and distributed processing engine for stateful computations over unbounded and bounded data streams. Flink has been designed to run in all common cluster environments, perform computations at in-memory speed, and at any scale. It can process both bounded and unbounded data streams. For batch processing, which uses the DataSet API, we deal with bounded data streams. For unbounded data streams, meaning streaming data from sources like Kafka or databases, we use the DataStream API for real-time data processing.

In our analysis, we will perform various computations on our data.

#### Problem

We have two datasets: order_items.csv and products.csv with the following columns:

order_items.csv: "OrderItemID", "OrderID", "ProductID", "Quantity", "PricePerUnit"
products.csv: "ProductID", "Name", "Description", "Price", "Category"
Both datasets have the "ProductID" column in common. Using these datasets, we will model our data to summarize sales based on categories, calculate total sales (by multiplying Quantity by PricePerUnit), and count items sold by category.

#### Key Questions to Answer

Which is the best-performing category in terms of revenue generated?
Which is the best-performing category in terms of purchases made?
Is the number of purchases made directly related to the revenue generated (i.e., are there goods that were purchased less but generated higher revenue)?
Which products in each category are generating the most revenue?
Are there products that are generating no revenue?
Based on these findings, we can identify reasons and make business decisions.

#### Project Plan

Flink runs on Java, and we will use Java 11 Runtime for this project. Other Java runtimes may have configuration issues, so Java 11 is recommended.

We will use the following configurations for our project:

Ensure that Flink is installed on your system. You should be able to access the Flink UI at localhost:8081 after running the following command:

bash



The code in the Product class is
```
package sales.entities;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Product {
    public Integer ProductID;
    public String Name;
    public String Description;
    public Float Price;
    public String Category;
}
```
Basically here is brief explanation of what the code is all about

The lombok class basically dcicates our getters and settters ,they dictates wahat can be accessed and read and setters help control what can be written 

The code in the oOrderItem

```
package sales.entities;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class OrderItem {
    public Integer OrderItemID;
    public Integer OrderID;
    public Integer ProductID;
    public Integer Quantity;
    public Float PricePerUnit;
}
```
The code in the Categorysalesdto

```
package sales.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategorySalesdto {
    public String Category;
    public Float totalSales;
    public Integer count;
}
```
This will be the results thta we will get from the flink job 

Remember when we said we will be looking for the total sales based on category and find the count of items sold


The code for the DataBatchJob

```


package sales;

import org.apache.flink.api.common.functions.JoinFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.api.common.io.OutputFormat;
import org.apache.flink.api.common.operators.Order;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.operators.DataSource;
import org.apache.flink.api.java.tuple.Tuple6;
import org.apache.flink.configuration.Configuration;
import sales.dto.CategorySalesdto;
import sales.entities.OrderItem;
import sales.entities.Product;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;


public class DataBatchJob {

	public static void main(String[] args) throws Exception {

		final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

		DataSource<OrderItem>orderitems =env.readCsvFile("/home/stilinski/IdeaProjects/sales/dataset/order_items.csv")
				.ignoreFirstLine().pojoType(OrderItem.class,"OrderItemID","OrderID","ProductID","Quantity","PricePerUnit");

		DataSource<Product>products  =env.readCsvFile("/home/stilinski/IdeaProjects/sales/dataset/products.csv")
				.ignoreFirstLine().pojoType(Product.class,"ProductID","Name","Description","Price","Category");

		//join the datasets
		DataSet<Tuple6<String,String,Float,Integer,Float,String>>joined = orderitems.join(products).where("ProductID")
				.equalTo("ProductID").with((JoinFunction<OrderItem, Product, Tuple6<String,String,Float,Integer,Float,String>>)(first,second)
		-> new Tuple6<>(
				second.ProductID.toString(),
						second.Name,
						first.PricePerUnit,
						first.Quantity,
						first.PricePerUnit * first.Quantity,
						second.Category
						)).returns(TypeInformation.of(new TypeHint<Tuple6<String, String, Float, Integer, Float, String>>() {}));

		//group by category to get total sales and count
		DataSet<CategorySalesdto> categorySales= joined.map((MapFunction<Tuple6<String, String, Float, Integer, Float, String>, CategorySalesdto> )record
						->new CategorySalesdto(record.f5, record.f4, 1)).returns(CategorySalesdto.class)
				.groupBy("Category").reduce( (ReduceFunction <CategorySalesdto>)(value1, value2)
				->new  CategorySalesdto(value1.getCategory(),value1.getTotalSales()+value2.getTotalSales(),value1.getCount()+value2.getCount()));


		//
		categorySales.output(new OutputFormat<CategorySalesdto>() {

			private transient BufferedWriter writer;
			@Override
			public void configure(Configuration configuration) {

			}
			@Override
			public void open(int taskNumber,int numTasks) throws IOException{
				File outputfile = new File("/home/stilinski/IdeaProjects/sales/dataset/results.csv");
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
		});


		//sort bu total sales in descending order
		categorySales.sortPartition("totalSales", Order.DESCENDING).print();



		// Execute program, beginning computation.
		env.execute("Sales Analysis");
	}}


```
##### Setup the Environment:

It creates an execution environment for the Flink job to run in. This environment manages the job execution.

##### Read CSV Files:

It reads two CSV files: order_items.csv and products.csv.
The order_items.csv file contains order details with columns like OrderItemID, OrderID, ProductID, Quantity, and PricePerUnit.
The products.csv file contains product details with columns like ProductID, Name, Description, Price, and Category.

##### Join the Datasets:

It joins the orderitems dataset with the products dataset based on the ProductID field that is common to both datasets.
The result of the join operation is a new dataset with tuples containing: ProductID, Name, PricePerUnit, Quantity, TotalPrice (calculated as PricePerUnit * Quantity), and Category.

##### Calculate Category Sales:

It maps the joined dataset to a new dataset where each record contains Category, TotalSales (initialized to the TotalPrice of that record), and Count (initialized to 1).
It groups the mapped dataset by Category and reduces it to calculate the total sales and count of items sold for each category.  

##### Output the Results:

It defines a custom output format to write the results to a CSV file results.csv.
The output file is opened in append mode, and for each record in the dataset, it writes the Category, TotalSales, and Count to the file.

##### Sort and Print Results:

It sorts the categorySales dataset by TotalSales in descending order and prints the sorted results.

##### Execute the Job:

It triggers the execution of the Flink job by calling env.execute("Sales Analysis").

After the code is complete to run the code you will need to 
```
mvn compile
```
then

```
mvn package
```
then a jar file will be created in a target folder run the folllowing command
```
~/"flink pathon your system"/bin/flink run -c sales.DataBatchJob target/sales-1.0-SNAPSHOT.jar
```
move to the Flink UI a and the job will be visible there
