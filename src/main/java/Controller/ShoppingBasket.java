package Controller;

import Database.myDB;

import java.util.*;
import java.util.Map.Entry;

/**
* Container for items to be purchased
*/
public class ShoppingBasket {
    // values will store the initial items' cost
    HashMap<String, Double> values;
    String[] names = {"apple", "orange", "pear", "banana"};
    String user;


    /**
    * Creates a new, empty ShoppingBasket object
    */
    public ShoppingBasket(String user) {
        this.values = new HashMap<>();
        this.user = user;

        this.values.put("apple", 2.5);
        this.values.put("orange", 1.25);
        this.values.put("pear", 3.00);
        this.values.put("banana", 4.95);


        if (myDB.userLogged(user)){
            Map<String,Double> newValues = myDB.getItemsAndCosts(user);
            for (String itemName : newValues.keySet()) {
                Double cost = newValues.get(itemName);
                this.values.put(itemName,cost);
            }
        }
        else {
            // init the cart
            for (String name: names) {
                //this.items.put(name, 0);
                myDB.AddItem(user,name,0,values.get(name));
            }
        }

    }

    /**
    * Adds an item to the shopping basket.
     *
     * @param item  The item to be added. Must match one of ‘apple’, ‘orange’, ‘pear’, or ‘banana’, in any case.
     * @param count The count of the item to be added. Must be 1 or more. It
     * allows only Integer.INT_MAX number of items of a kind to be stored. If
     * items are added after INT_MAX, the parameter requirements will be breached.
     * @throws IllegalArgumentException If any parameter requirements are breached.
     */
    public void addItem(String item, int count) throws IllegalArgumentException {
        if (item == null) throw new IllegalArgumentException("Item is invalid");
        String stringItem = item.toLowerCase();

        //if (!this.items.containsKey(stringItem)) throw new IllegalArgumentException("Item " + stringItem + " is not present.");
        if (!myDB.getItems(user).contains(stringItem)) throw new IllegalArgumentException("Item " + stringItem + " is not present.");
        if (count < 1) throw new IllegalArgumentException("Item " + item + " has invalid count.");

        // Integer itemVal = this.items.get(stringItem);
        int itemVal = myDB.getCountByUserAndItem(user,stringItem);
        double cost = myDB.getCostByUserAndItem(user,stringItem);
        if (itemVal == Integer.MAX_VALUE) throw new IllegalArgumentException("Item " + item + " has reached maximum count.");

        //this.items.put(stringItem, itemVal + count);
        myDB.AddItem(user,stringItem,itemVal + count,cost);
    }

    public void addNewItem(String item, double price) throws IllegalArgumentException{
        if (item == null) throw new IllegalArgumentException("Item is invalid");
        if (price <= 0) throw new IllegalArgumentException("price is invalid");
        if (item == "") throw new IllegalArgumentException("Item is invalid");

        List<String> list = new ArrayList<String>(Arrays.asList(names));

        if (!list.contains(item.toLowerCase())) {
            this.values.put(item.toLowerCase(), price);
            //this.items.put(item.toLowerCase(), 0);
            myDB.AddItem(user,item,0,price);

            list.add(item.toLowerCase());
            names = list.toArray(new String[0]);
        }
    }

    /**
     * Removes an item from the shopping basket, based on a case-insensitive but otherwise exact match.
     *
     * @param count The count of the item to be added. Must be 1 or more.
     * @return False if the item was not found in the basket, or if the count was higher than the amount of this item currently present, otherwise true.
     * @throws IllegalArgumentException If any parameter requirements are breached.
     */
    public boolean removeItem(String item, int count) throws IllegalArgumentException {
        if (item == null) throw new IllegalArgumentException("Item is invalid");
        String stringItem = item.toLowerCase();

        //if (!this.items.containsKey(stringItem)) return false;
        if (!myDB.getItems(user).contains(stringItem)) return false;
        if (count < 1) throw new IllegalArgumentException(count + " is invalid count.");

        // Integer itemVal = this.items.get(stringItem);
        Integer itemVal = myDB.getCountByUserAndItem(user,stringItem);
        double cost = myDB.getCostByUserAndItem(user,stringItem);

        Integer newVal = itemVal - count;
        if (newVal < 0) return false;
        //this.items.put(stringItem, newVal);
        myDB.AddItem(user,stringItem,newVal,cost);
        return true;
    }

    /**
    * Gets the contents of the ShoppingBasket.
    *
    * @return A list of items and counts of each item in the basket. This list is a copy and any modifications will not modify the existing basket.
    */
    public List<Entry<String, Integer>> getItems() {
//        ArrayList<Entry<String, Integer>> originalItems = new ArrayList<Entry<String, Integer>>(this.items.entrySet());
//        ArrayList<Entry<String, Integer>> copyItems = new ArrayList<Entry<String, Integer>>();
//
//        int index = 0;
//
//        for(Entry<String,Integer> entry: originalItems){
//            copyItems.add(index, Map.entry(entry.getKey(), entry.getValue()));
//            index++;
//        }
        List<Entry<String, Integer>> copyItems = myDB.getItem(user);

        return copyItems;
    }

    /**
    * Gets the current dollar value of the ShoppingBasket based on the following values: Apples: $2.50, Oranges: $1.25, Pears: $3.00, Bananas: $4.95
    *
    * @return Null if the ShoppingBasket is empty, otherwise the total dollar value.
    */
    public Double getValue() {
        Double val = 0.0;

        for (String name: names) {
           //val += this.values.get(name) * this.items.get(name);
            val += myDB.getCostByUserAndItem(user,name) * myDB.getCountByUserAndItem(user,name);
        }

        if (val == 0.0) return null;
        return val;
    }

    /**
    * Empties the ShoppingBasket, removing all items.
    */
    public void clear() {
        for (String name: names) {
            double cost = myDB.getCostByUserAndItem(user,name);
            myDB.AddItem(user,name,0,cost);
        }
    }

    /**
    * update a itme's count
    */
    public void updateItemValue(String item, Integer count){
        if (item == null) throw new IllegalArgumentException("Item is invalid");
        //int oldValue = this.items.get(item);
        int oldValue = myDB.getCountByUserAndItem(user,item);
        if (oldValue > count){
            this.removeItem(item,oldValue-count);
        } else if (oldValue < count) {
            this.addItem(item,count-oldValue);
        }
    }

    /**
     * delete the item name from items
     */

    public void delItem(String item){
        if (item == null) throw new IllegalArgumentException("Item is invalid");
        //this.items.remove(item);
        myDB.removeItem(item,user);
        this.values.remove(item);
        List<String> list = new ArrayList<String>(Arrays.asList(names));
        list.remove(item);
        names = list.toArray(new String[0]);
    }

    public void updateNameCost(String[] item, double[] cost){
        List<Entry<String, Integer>> oldItems = this.getItems();
        int index = 0;
        for (Entry<String, Integer> oldItem:oldItems) {
            double oldCost = values.get(oldItem.getKey());
            int value = oldItem.getValue();
            if (item[index] == null) throw new IllegalArgumentException("Item is invalid");
            if (cost[index] <= 0) throw new IllegalArgumentException("cost is invalid");
            if (item[index] == "") throw new IllegalArgumentException("Item is invalid");

            if (!item[index].equals(oldItem.getKey())){
                //this.items.remove(oldItem.getKey());

                myDB.removeItem(oldItem.getKey(),user);
                this.values.remove(oldItem.getKey());
                List<String> list = new ArrayList<String>(Arrays.asList(names));
                list.remove(oldItem.getKey());
                list.add(item[index]);
                names = list.toArray(new String[0]);
                //this.items.put(item[index],value);
                myDB.AddItem(user,item[index],value,cost[index]);

                this.values.put(item[index],cost[index]);
            }
            else{
                this.values.put(item[index],cost[index]);
            }
            index++;
        }



    }
}
