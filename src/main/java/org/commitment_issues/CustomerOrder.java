package org.commitment_issues;

import org.json.*;
import java.util.*;

public class CustomerOrder {
	public String customerName;
	public String customerId;
	public int customerType;
	public float customerLocationX;
	public float customerLocationY;
	
	public String orderID;
	public int orderDay;
	public int orderTime;
	
	public int deliveryDay;
	public int deliveryTime;
	
	public Hashtable<String, Integer> productList = new Hashtable<String, Integer>();
	
	public void setProductList(Hashtable<String, Integer> input) {
		this.productList = input;
	}
	
	public Hashtable<String, Integer> getProductList() {
		return this.productList;
	}

}
