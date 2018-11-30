package org.commitment_issues.delivery_agents;

import java.util.ArrayList;

public class Order {
  private String orderID;
  private ArrayList<Box> boxes = new ArrayList<Box>();
  private float[] location = new float[2];
  private float[] destination = new float[2];
  
  public Order(String orderID, ArrayList<Box> boxes, float[] location, float[] destination) {
    this.orderID = orderID;
    this.boxes = boxes;
    this.location = location;
    this.destination = destination;
  }

  public float[] getDestination() {
    return destination;
  }

  public void setDestination(float[] destination) {
    this.destination = destination;
  }

  public float[] getLocation() {
    return location;
  }

  public void setLocation(float[] location) {
    this.location = location;
  }

  public Order() {
    super();
  }
  public String getOrderID() {
    return this.orderID;
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
  
  public void addBoxes(Box boxes) {
    this.boxes.add(boxes);
  }
}