# Delivery Stage messages
## OrderAggregator to TransportAgent

### Example message:
```
  [
    {
      "OrderID": "Order-123",
      "Boxes": [
        {
          "BoxID": "1",
          "ProductType":"1",
          "Quantity":"12"
        },
                {
          "BoxID": "2",
          "ProductType":"5",
          "Quantity":"1"
        }
      ]
    },
    {
      "OrderID": "Order-002",
      "Boxes": [
        {
          "BoxID": "1",
          "ProductType":"1",
          "Quantity":"12"
        },
                {
          "BoxID": "2",
          "ProductType":"5",
          "Quantity":"1"
        }
      ]
    }
  ]
```

<br>

## TransportAgent to TruckAgent (CFP)

### Example message:

```
{
  "OrderID": "Order-123",
  "Source": {
    "X": 2.358,
    "Y": 6.895
  },
  "Destination": {
    "X": 2.358,
    "Y": 6.895
  },
  "NumOfBoxes": 12
}
```
<br>

## TruckAgent to StreetNetworkAgent (REQUEST)

### Example message:

```
{
  "Source": {
    "X": 2.358,
    "Y": 6.895
  },
  "Destination": {
    "X": 2.358,
    "Y": 6.895
  }
}
```
<br>

## StreetNetworkAgent to TruckAgent (Reply)
### Example message:
```
{
  "Time": 2.356
}
```
<br>

## TruckAgent to TransportAgent (Reply)
### Example message:
```
{
  "OrderID": "Order-123",
  "Time": 2.356
}
```
<br>

## TruckAgent to TransportAgent (Request)
### Example message:
```
{
  "OrderID": "Order-123"
}
```
<br>

## TransportAgent to TruckAgent (Reply)
### Example message:
```
{
  "OrderID": "Order-123",
  "Boxes": [
    {
      "BoxID": "1",
      "ProductType": "1",
      "Quantity": "12"
    },
    {
      "BoxID": "2",
      "ProductType": "5",
      "Quantity": "1"
    }
  ]
}
```
<br>