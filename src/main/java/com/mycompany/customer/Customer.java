/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */
package com.mycompany.customer;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import io.github.cdimascio.dotenv.Dotenv;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 *
 * @author FAUZI
 */
public class Customer {

    private static MongoCollection<Document> customerCollection;
    private static Scanner scanner;

    public static void main(String[] args) {
        scanner = new Scanner(System.in);

        try {
            System.out.println("Trying to load .env file...");
            Dotenv dotenv = Dotenv.configure()
                    .directory(".")
                    .load();

//            String uri = dotenv.get("MONGODB_URI");
//            System.out.println("DEBUG: MONGODB_URI = " + uri);

            String connectionString = dotenv.get("MONGODB_URI");
            String dbName = dotenv.get("DB_NAME");
            String collectionName = dotenv.get("COLLECTION_NAME");

            if (connectionString == null || connectionString.isEmpty()) {
                throw new IllegalStateException("MONGODB_URI is not set in .env");
            }
            if (dbName == null || dbName.isEmpty()) {
                throw new IllegalStateException("DB_NAME is not set in .env");
            }
            if (collectionName == null || collectionName.isEmpty()) {
                throw new IllegalStateException("COLLECTION_NAME is not set in .env");
            }

//            System.out.println("Connecting to MongoDB at: " + connectionString);
//            System.out.println("Database: " + dbName);
//            System.out.println("Collection: " + collectionName);


            MongoClient mongoClient = MongoClients.create(connectionString);
            MongoDatabase database = mongoClient.getDatabase(dbName);
            customerCollection = database.getCollection(collectionName);

            System.out.println("Connected to MongoDB successfully!");

            boolean running = true;
            while (running) {
                displayMenu();
                int choice = getIntInput();

                switch (choice) {
                    case 1:
                        addCustomer();
                        break;
                    case 2:
                        viewAllCustomers();
                        break;
                    case 3:
                        searchCustomer();
                        break;
                    case 4:
                        updateCustomer();
                        break;
                    case 5:
                        deleteCustomer();
                        break;
                    case 6:
                        running = false;
                        System.out.println("Exiting application...");
                        mongoClient.close();
                        break;
                    default:
                        System.out.println("Invalid choice. Please try again.");
                }
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void displayMenu() {
        System.out.println("\n==== Customer Management System ====");
        System.out.println("1. Add New Customer");
        System.out.println("2. View All Customers");
        System.out.println("3. Search Customer");
        System.out.println("4. Update Customer");
        System.out.println("5. Delete Customer");
        System.out.println("6. Exit");
        System.out.print("Enter your choice: ");
    }

    private static int getIntInput() {
        try {
            return Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static void addCustomer() {
        System.out.println("\n-- Add New Customer --");
        System.out.print("Enter name: ");
        String name = scanner.nextLine();

        System.out.print("Enter email: ");
        String email = scanner.nextLine();

        System.out.print("Enter phone: ");
        String phone = scanner.nextLine();

        Document customer = new Document()
                .append("name", name)
                .append("email", email)
                .append("phone", phone)
                .append("createdAt", new java.util.Date());

        customerCollection.insertOne(customer);
        System.out.println("Customer added successfully!");
    }

    private static void viewAllCustomers() {
        System.out.println("\n-- All Customers --");
        List<Document> customers = new ArrayList<>();
        customerCollection.find().into(customers);

        if (customers.isEmpty()) {
            System.out.println("No customers found.");
            return;
        }

        for (Document customer : customers) {
            printCustomer(customer);
        }
    }

    private static void searchCustomer() {
        System.out.println("\n-- Search Customer --");
        System.out.print("Enter customer name or email to search: ");
        String searchTerm = scanner.nextLine();

        Document query = new Document();
        query.append("$or", List.of(
                new Document("name", new Document("$regex", searchTerm).append("$options", "i")),
                new Document("email", new Document("$regex", searchTerm).append("$options", "i"))
        ));

        List<Document> customers = new ArrayList<>();
        customerCollection.find(query).into(customers);

        if (customers.isEmpty()) {
            System.out.println("No customers found matching your search.");
            return;
        }

        System.out.println("\nFound " + customers.size() + " customer(s):");
        for (Document customer : customers) {
            printCustomer(customer);
        }
    }

    private static void updateCustomer() {
        System.out.println("\n-- Update Customer --");
        System.out.print("Enter customer ID to update: ");
        String id = scanner.nextLine();

        try {
            ObjectId objectId = new ObjectId(id);
            Document customer = customerCollection.find(Filters.eq("_id", objectId)).first();

            if (customer == null) {
                System.out.println("Customer not found.");
                return;
            }

            System.out.println("Current customer details:");
            printCustomer(customer);

            System.out.println("\nEnter new details (leave blank to keep current value):");

            System.out.print("Enter name [" + customer.getString("name") + "]: ");
            String name = scanner.nextLine();
            name = name.isEmpty() ? customer.getString("name") : name;

            System.out.print("Enter email [" + customer.getString("email") + "]: ");
            String email = scanner.nextLine();
            email = email.isEmpty() ? customer.getString("email") : email;

            System.out.print("Enter phone [" + customer.getString("phone") + "]: ");
            String phone = scanner.nextLine();
            phone = phone.isEmpty() ? customer.getString("phone") : phone;

            customerCollection.updateOne(
                    Filters.eq("_id", objectId),
                    Updates.combine(
                            Updates.set("name", name),
                            Updates.set("email", email),
                            Updates.set("phone", phone),
                            Updates.set("updatedAt", new java.util.Date())
                    )
            );

            System.out.println("Customer updated successfully!");
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid ID format.");
        }
    }

    private static void deleteCustomer() {
        System.out.println("\n-- Delete Customer --");
        System.out.print("Enter customer ID to delete: ");
        String id = scanner.nextLine();

        try {
            ObjectId objectId = new ObjectId(id);
            Document customer = customerCollection.find(Filters.eq("_id", objectId)).first();

            if (customer == null) {
                System.out.println("Customer not found.");
                return;
            }

            System.out.println("Customer to delete:");
            printCustomer(customer);

            System.out.print("Are you sure you want to delete this customer? (y/n): ");
            String confirm = scanner.nextLine();

            if (confirm.equalsIgnoreCase("y")) {
                customerCollection.deleteOne(Filters.eq("_id", objectId));
                System.out.println("Customer deleted successfully!");
            } else {
                System.out.println("Delete operation cancelled.");
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid ID format.");
        }
    }

    private static void printCustomer(Document customer) {
        System.out.println("\nID: " + customer.getObjectId("_id"));
        System.out.println("Name: " + customer.getString("name"));
        System.out.println("Email: " + customer.getString("email"));
        System.out.println("Phone: " + customer.getString("phone"));
        System.out.println("Created: " + customer.getDate("createdAt"));
        if (customer.containsKey("updatedAt")) {
            System.out.println("Updated: " + customer.getDate("updatedAt"));
        }
        System.out.println("------------------------");
    }
}
