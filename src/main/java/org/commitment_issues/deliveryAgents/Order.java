package org.commitment_issues.deliveryAgents;

import java.util.ArrayList;

public class Order {
  private String orderID;
  private ArrayList<Box> boxes;
  
  public Order(String orderID, ArrayList<Box> boxes) {
    this.orderID = orderID;
    this.boxes = boxes;
  }

  public String getOrderID() {
    return orderID;
  }

  public void setOrderID(String orderID) {
    this.orderID = orderID;
  }

  public ArrayList<Box> getBoxes() {
    return boxes;
  }

  public void setBoxes(ArrayList<Box> boxes) {
    this.boxes = boxes;
  }
}
