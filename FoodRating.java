package cache.lru;

import java.util.*;

public class FoodRatingApp {
    /*
    Eater should be able to add or modify the rating.
    Eater can also query top N food by ratings
    There can be multiple restaurants
     */

    public static class Restaurant
    {
        String name;
        String location;
        Map<String, FoodItem> foodItemsMap;
        TreeSet<FoodItem> foodItemSet;
        public Restaurant(String name, String location)
        {
            this.name = name;
            this.location = location;
            this.foodItemsMap = new HashMap<>();
            this.foodItemSet = new TreeSet<>((item1, item2) -> {
                int ratingCompare = Double.compare(item2.averageRating, item1.averageRating);
                return ratingCompare != 0 ? ratingCompare : item1.name.compareTo(item2.name);
            });
        }

        public void addOrModifyRating(String userId, String foodName, double rating)
        {
            // TC: O(log n)

            FoodItem foodItem = foodItemsMap.getOrDefault(foodName, new FoodItem(foodName));
            if (foodItemsMap.containsKey(foodName)) {
                foodItemSet.remove(foodItem);
            }

            foodItem.addOrModifyRating(userId, rating);
            foodItemsMap.put(foodName, foodItem);

            foodItemSet.add(foodItem); // re-adding to update rating

        }

        public List<FoodItem> getTopRatedFoodItems(int k)
        {
            // TC: O(n)

            List<FoodItem> topRatedFoodItems = new ArrayList<>();
            Iterator<FoodItem> iterator = foodItemSet.iterator();
            while (iterator.hasNext() && topRatedFoodItems.size() < k) {
                topRatedFoodItems.add(iterator.next());
            }
            return topRatedFoodItems;
        }
    }

    public static class FoodItem
    {
        String name;
        double averageRating;
        Map<String,Double> userVsRatingMap;
        int numberOfRatings;
        public FoodItem(String name)
        {
            this.name = name;
            this.averageRating = 0;
            this.numberOfRatings = 0;
            this.userVsRatingMap = new HashMap<>();
        }

        public void addOrModifyRating(String userId, double rating)
        {
            if(userVsRatingMap.isEmpty())
            {
                averageRating = rating;
                numberOfRatings++;
                userVsRatingMap.put(userId, rating);
                return;
            }

            if(userVsRatingMap.containsKey(userId))
            {
                if(userVsRatingMap.size()==1)
                {
                    averageRating = rating;
                    userVsRatingMap.put(userId, rating);
                    return;
                }
                double oldRating = userVsRatingMap.get(userId);
                // update average rating - remove old one and add new one
                double newAverage = ((averageRating * numberOfRatings) - oldRating)/ (numberOfRatings-1);
                averageRating = ((newAverage * numberOfRatings) + rating)/ (numberOfRatings+1);
            }
            else {
                averageRating = ((averageRating * numberOfRatings) + rating)/ (numberOfRatings+1);
                numberOfRatings++;
            }
            userVsRatingMap.put(userId, rating);
        }
    }

    public static void printList(List<FoodItem> list)
    {
        System.out.println("Top Rated Food Items: ");
        list.forEach(foodItem -> {
            System.out.println(foodItem.name + " - Rating: " + String.format("%.2f", foodItem.averageRating));
        });
    }

    public static void main(String[] args) {
        Restaurant restaurant = new Restaurant("Food Rating App", "Vijayawada");
        restaurant.addOrModifyRating("abcd", "pizza", 4);
        restaurant.addOrModifyRating("abc1d", "pizza", 5);
        restaurant.addOrModifyRating("abc1d", "burger", 5);
        restaurant.addOrModifyRating("abc1d", "burger", 3);
        restaurant.addOrModifyRating("user3", "burger", 1);
        restaurant.addOrModifyRating("user4", "burger", 4);
        restaurant.addOrModifyRating("user4", "biryani", 4);
        restaurant.addOrModifyRating("user4", "dosa", 4);
        printList(restaurant.getTopRatedFoodItems(5));
    }
}
