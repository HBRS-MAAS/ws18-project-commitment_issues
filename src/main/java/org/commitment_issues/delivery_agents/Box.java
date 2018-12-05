package org.commitment_issues.delivery_agents;

import org.json.JSONObject;

public class Box {
	private String boxID;
	private String productType;
	private int quantity;
	private int capacity;

	public Box(String boxID, String productType, int quantity, int capacity) {
		this.boxID = boxID;
		this.productType = productType;
		this.quantity = quantity;
		this.capacity = capacity;
	}

	public Box() {
		super();
	}
	
	public void printDetails() {
		System.out.println("********* Box: " + boxID + " *********");
		System.out.println("Product Type: " + productType);
		System.out.println("Capacity: " + capacity);
		System.out.println("Current items count: " + quantity);
		System.out.println("**********************************");
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
	
	public int addItems(int quantity) {
		int remainingItems;
		int freeSpace = getFreeSpace();
		if (freeSpace > quantity) {
			remainingItems = 0;
			this.quantity += quantity;
		}
		else {
			remainingItems = quantity - freeSpace;
			this.quantity += freeSpace;
		}
		
		return remainingItems;
	}
	
	public String getJsonString() {		
		return getAsJSONObject().toString();
	}
	
	public int getFreeSpace() {
		return capacity - quantity;
	}
	
	public JSONObject getAsJSONObject() {
		JSONObject obj = new JSONObject();
		obj.put("BoxID", boxID);
		obj.put("ProductType", productType);
		obj.put("Quantity", quantity);
		return obj;
	}

}