package data.entities;


import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Product
{
    public Integer ProductID;
    public String Name;
    public String Description;
    public Float Price;
    public String Category;

}
