package data.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
@AllArgsConstructor
public class CategorySalesdto{
    public String Category;
    public Float totalSales;
    public Integer count;

}
