package data.entities;

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
