package org.commitment_issues.delivery_agents;

public class Box {
  private String boxID;
  private String productType;
  private int quantity;
  public Box(String  boxID, String productType, int quantity) {
    this.boxID = boxID;
    this.productType = productType;
    this.quantity = quantity;
  }
  public Box() {
    super();
  }
  public String getBoxID() {
    return boxID;
  }
  public void setBoxID(String boxID) {
    this.boxID = boxID;
  }
  public String getProductType() {
    return productType;
  }
  public void setProductType(String productType) {
    this.productType = productType;
  }
  public int getQuantity() {
    return quantity;
  }
  public void setQuantity(int quantity) {
    this.quantity = quantity;
  }
  
}