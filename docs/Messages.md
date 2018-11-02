# OrderMessage
## Customer requests a order processor of a bakery for some products to be delivered at specific time
### ""
<br>
# Status
## The order processor acknowledges the receipt of the order form the customer
###  ""
<br>
# requestForPendingOrders
## The production manager requests todays orders from the order processor
###  ""
<br>
# todaysOrders
## The Order processor informs the production manager the list of products due for today
###  ""
<br>
# requestProductInfo
## The production manager requests the order for the list of products
### ""
<br>
# orderInfo
## The order provides the list of all products to be fullfilled to the production manager
### ""
<br>
# getTimeToDelivery
## The product manager requests the time to tranport an order
###  ""
<br>
# assignKneadingTask
## The production manager informs the kneading agent about the quantity of dough to be kneaded
### ""
<br>
# kneadingComplete
## The kneading agent  is ready for kneading
### ""
<br>
# restingComplete
## The previously kneaded dough is ready for preparation
### ""
<br>
# assignPrepTask
## Request the item preparation agents to prepare the items
### ""
<br>
# preparationComplete
## The preparation agent informs the production manager that it is ready to prepare new items
### ""
<br>
# assignBakingTask
## The production manager requests the baking agent to bake the prepared items
### ""
<br>
# BakingComplete
## The baking agent informs the production manager that baking is complete and oven is free
### ""
<br>
# itemsReadyToPack
## The baking agent informs the packaging agent that the items are cooled down and ready for packaging
### ""
<br>
# itemsPacked
## The packing agent informs the Order tracker that some items have been packed
### ""
<br>
# moveOrderAgent
## The production manager passes the responsibility of the order agent to the order tracker agent
### ""
<br>
# updateCompletedProducts
## The order tracker informs the order agent that some of its products are ready for dispatch
### ""
<br>
# OrderFulfilled
## The order agent informs the order tracker that the order is ready for delivery
### ""
<br>
# GetDeliveryAddress
## The Order tracker request the order agent for the delivery address
### ""
<br>
# DeliveryAddress
## The order informs the order tracker about the delivery address
### ""
<br>
# RequestOrderTransport
## The order tracker requests transporation of a complete order
### ""
<br>
# queryTimeToDelivery
## The Transport agent requests the Street Network agent for time to move a truck between the bakery and the customer
### ""
<br>
# deliverOrders
## The transport agent requests a truck to transport some orders
### ""
<br>
# DeliveryStatus
## The truck informs the order that the products were delivered to the customer. The order can then die.
### ""
<br>