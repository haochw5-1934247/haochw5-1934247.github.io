package scheduler;

import scheduler.db.ConnectionManager;
import scheduler.model.Caregiver;
import scheduler.model.Patient;
import scheduler.model.Vaccine;
import scheduler.util.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;

public class Scheduler {

    // objects to keep track of the currently logged-in user
    // Note: it is always true that at most one of currentCaregiver and currentPatient is not null
    //       since only one user can be logged-in at a time
    private static Caregiver currentCaregiver = null;
    private static Patient currentPatient = null;
    private static String reservingPatient = null;
    private static String reservingCaregiver = null;

    public static void main(String[] args) {
        // printing greetings text
        System.out.println();
        System.out.println("Welcome to the COVID-19 Vaccine Reservation Scheduling Application!");
        System.out.println("*** Please enter one of the following commands ***");
        System.out.println("> create_patient <username> <password>");  //TODO: implement create_patient (Part 1)
        System.out.println("> create_caregiver <username> <password>");
        System.out.println("> login_patient <username> <password>");  // TODO: implement login_patient (Part 1)
        System.out.println("> login_caregiver <username> <password>");
        System.out.println("> search_caregiver_schedule <date>");  // TODO: implement search_caregiver_schedule (Part 2)
        System.out.println("> reserve <date> <vaccine>");  // TODO: implement reserve (Part 2)
        System.out.println("> upload_availability <date>");
        System.out.println("> cancel <appointment_id>");  // TODO: implement cancel (extra credit)
        System.out.println("> add_doses <vaccine> <number>");
        System.out.println("> show_appointments");  // TODO: implement show_appointments (Part 2)
        System.out.println("> logout");  // TODO: implement logout (Part 2)
        System.out.println("> quit");
        System.out.println();

        // read input from user
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("> ");
            String response = "";
            try {
                response = r.readLine();
            } catch (IOException e) {
                System.out.println("Please try again!");
            }
            // split the user input by spaces
            String[] tokens = response.split(" ");
            // check if input exists
            if (tokens.length == 0) {
                System.out.println("Please try again!");
                continue;
            }
            // determine which operation to perform
            String operation = tokens[0];
            if (operation.equals("create_patient")) {
                createPatient(tokens);
            } else if (operation.equals("create_caregiver")) {
                createCaregiver(tokens);
            } else if (operation.equals("login_patient")) {
                loginPatient(tokens);
            } else if (operation.equals("login_caregiver")) {
                loginCaregiver(tokens);
            } else if (operation.equals("search_caregiver_schedule")) {
                searchCaregiverSchedule(tokens);
            } else if (operation.equals("reserve")) {
                reserve(tokens);
            } else if (operation.equals("upload_availability")) {
                uploadAvailability(tokens);
            } else if (operation.equals("cancel")) {
                cancel(tokens);
            } else if (operation.equals("add_doses")) {
                addDoses(tokens);
            } else if (operation.equals("show_appointments")) {
                showAppointments(tokens);
            } else if (operation.equals("logout")) {
                logout(tokens);
            } else if (operation.equals("quit")) {
                System.out.println("Bye!");
                return;
            } else {
                System.out.println("Invalid operation name!");
            }
        }
    }

    private static void createPatient(String[] tokens) {
        // Todo: Part 1 (Done with extra credit 1)
        // create_patient <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 2: check if the username has been taken already
        if (usernameExistsPatient(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        // The password must be at least 8 characters.
        if (!(password.length() >= 8)) {
            System.out.println("Password should be at least 8 letters, please try again!");
            return;
        }
        // The password must be a mixture of both uppercase and lowercase letters.
        if (password.equals(password.toLowerCase()) || password.equals(password.toUpperCase())) {
            System.out.println("Password should be a mixture of both uppercase and lowercase letters, please try again!");
            return;
        }
        // The password must be A mixture of letters and numbers.
        if (password.matches("[a-zA-Z]+") || password.matches("[0-9]+")) {
            System.out.println("Password should be a mixture of letters and numbers, please try again!");
            return;
        }
        // The password must include at least one special character, from “!”, “@”, “#”, “?”.
        if (!password.contains("!") && !password.contains("@") && !password.contains("#") && !password.contains("?")) {
            System.out.println("Password should include at least one special character from “!”, “@”, “#”, “?”, please try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the caregiver
        try {
            currentPatient = new Patient.PatientBuilder(username, salt, hash).build();
            // save to patient information to our database
            currentPatient.saveToDB();
            System.out.println(" *** Account created successfully *** ");
        } catch (SQLException e) {
            System.out.println("Create failed");
            e.printStackTrace();
        }
        reservingPatient = username;
        reservingCaregiver = null;
    }

    private static void createCaregiver(String[] tokens) {
        // create_caregiver <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 2: check if the username has been taken already
        if (usernameExistsCaregiver(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        // The password must be at least 8 characters.
        if (!(password.length() >= 8)) {
            System.out.println("Password should be at least 8 letters, please try again!");
            return;
        }
        // The password must be a mixture of both uppercase and lowercase letters.
        if (password.equals(password.toLowerCase()) || password.equals(password.toUpperCase())) {
            System.out.println("Password should be a mixture of both uppercase and lowercase letters, please try again!");
            return;
        }
        // The password must be A mixture of letters and numbers.
        if (password.matches("[a-zA-Z]+") || password.matches("[0-9]+")) {
            System.out.println("Password should be a mixture of letters and numbers, please try again!");
            return;
        }
        // The password must include at least one special character, from “!”, “@”, “#”, “?”.
        if (!password.contains("!") && !password.contains("@") && !password.contains("#") && !password.contains("?")) {
            System.out.println("Password should include at least one special character from “!”, “@”, “#”, “?”, please try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the caregiver
        try {
            currentCaregiver = new Caregiver.CaregiverBuilder(username, salt, hash).build();
            // save to caregiver information to our database
            currentCaregiver.saveToDB();
            System.out.println(" *** Account created successfully *** ");
        } catch (SQLException e) {
            System.out.println("Create failed");
            e.printStackTrace();
        }
        reservingPatient = null;
        reservingCaregiver = username;
    }

    private static boolean usernameExistsCaregiver(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        String selectUsername = "SELECT * FROM Caregivers WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static boolean usernameExistsPatient(String username) {
        // Todo: Part 1 (Method used in createPatient)
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        String selectUsername = "SELECT * FROM Patients WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet getResult = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return getResult.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static void loginPatient(String[] tokens) {
        // TODO: Part 1
        // login_patient <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("Already logged-in!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Wrong input. Please try again!");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        Patient patient = null;
        try {
            patient = new Patient.PatientGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when logging in");
            e.printStackTrace();
        }
        // check if the login was successful
        if (patient == null) {
            System.out.println("Please try again!");
        } else {
            System.out.println("Patient logged in as: " + username);
            currentPatient = patient;
        }
        reservingPatient = username;
        reservingCaregiver = null;
    }

    private static void loginCaregiver(String[] tokens) {
        // login_caregiver <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("Already logged-in!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        Caregiver caregiver = null;
        try {
            caregiver = new Caregiver.CaregiverGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when logging in");
            e.printStackTrace();
        }
        // check if the login was successful
        if (caregiver == null) {
            System.out.println("Please try again!");
        } else {
            System.out.println("Caregiver logged in as: " + username);
            currentCaregiver = caregiver;
        }
        reservingPatient = null;
        reservingCaregiver = username;
    }

    private static void searchCaregiverSchedule(String[] tokens) {
        // TODO: Part 2 (done)
        // Can't continue if hasn't logged in yet.
        if (currentPatient == null && currentCaregiver == null) {
            System.out.println("You are not logged in. Please log in first!");
            return;
        }
        // Length of token must be 2.
        if (tokens.length != 2) {
            System.out.println("Wrong input. Please try again!");
            return;
        }
        String inputDate = tokens[1];
        try {
            Date availability = Date.valueOf(inputDate);
            System.out.print("Caregiver: ");
            searchCaregiver(availability);
            System.out.println();
            searchVaccine();
        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date!");
        }
    }

    private static void searchCaregiver(Date availability) {
        // Todo: Part 2 (Method used in searchCaregiverSchedule)
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        String selectUsername = "Select Username from Availabilities where Time = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setDate(1, availability);
            ResultSet getResult = statement.executeQuery();
            while (getResult.next()) {
                System.out.print(getResult.getString("Username"));
            }
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    }

    private static void searchVaccine() {
        // Todo: Part 2 (Method used in searchCaregiverSchedule)
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        String selectVaccine = "Select * from Vaccines";
        try {
            PreparedStatement statement = con.prepareStatement(selectVaccine);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                Vaccine vaccine = new Vaccine.VaccineGetter(resultSet.getString("Name")).get();
                System.out.println(vaccine.toString());
            }
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    }

    private static void reserve(String[] tokens) {
        // TODO: Part 2 (done)
        // Patient must be logged in before starting reservation.
        if (currentPatient == null) {
            System.out.println("Please login as a patient first!");
            return;
        }
        // Length of token must be 3.
        if (tokens.length != 3) {
            System.out.println("Wrong input. Please try again!");
            return;
        }
        String inputDate = tokens[1];
        String vaccine = tokens[2];
        try {
            Date availability = Date.valueOf(inputDate);
            if (!successCheckedIn(availability, vaccine)) {
                return;
            }
            String appointment = inputDate + reservingPatient;
            reservePatient(availability, vaccine, appointment, reservingPatient);
        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date and a valid vaccine!");
        }
    }

    private static boolean successCheckedIn(Date availability, String vaccine) {
        // TODO: Part 2 (method used in reserve)
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        String checkVaccine = "Select Doses from Vaccines where Name = ? and Doses >= 1";
        String checkCaregiver = "Select Username from Availabilities where Time = ?";
        try {
            PreparedStatement chooseVaccine = con.prepareStatement(checkVaccine);
            chooseVaccine.setString(1, vaccine);
            ResultSet resultVaccine = chooseVaccine.executeQuery();
            // Return false if there is no required vaccine available for the patient.
            if (!resultVaccine.next()) {
                System.out.println("There is no dose available for this vaccine on this date. Please try again!");
                return false;
            }
            PreparedStatement assignCaregiver = con.prepareStatement(checkCaregiver);
            assignCaregiver.setDate(1, availability);
            ResultSet resultCaregiver = assignCaregiver.executeQuery();
            // Return false if there is no caregiver available.
            if (!resultCaregiver.next()) {
                System.out.println("There is no caregiver available on this date. Please try another date!");
                return false;
            }
        } catch (SQLException e) {
            System.out.println("Error occurred when updating reservation");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static void reservePatient(Date availability, String vaccine, String appointment, String reservingPatient) {
        // TODO: Part 2 (method used in reserve)
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        String selectCaregiver = "Select top 1 * from Availabilities where Time = ? order by NEWID()";
        String makeAppointment = "Insert into Appointment values (? , ? , ? , ? , ?)";
        String deleteAvailability = "Delete from Availabilities where Time = ? and Username = ?";
        String CName = null;
        try {
            PreparedStatement statementCaregiver = con.prepareStatement(selectCaregiver);
            statementCaregiver.setDate(1, availability);
            ResultSet resultSet = statementCaregiver.executeQuery();
            while (resultSet.next()) {
                CName = resultSet.getString("Username");
            }
            PreparedStatement designAppointment = con.prepareStatement(makeAppointment);
            designAppointment.setString(1, appointment);
            designAppointment.setDate(2, availability);
            designAppointment.setString(3, CName);
            designAppointment.setString(4, vaccine);
            designAppointment.setString(5, reservingPatient);
            designAppointment.executeUpdate();
            System.out.println("Your assigned caregiver is " + CName);
            System.out.println("Your appointment ID is " + appointment);
            Vaccine vaccineDoses = new Vaccine.VaccineGetter(vaccine).get();
            vaccineDoses.decreaseAvailableDoses(1);
            PreparedStatement statementDelete = con.prepareStatement(deleteAvailability);
            statementDelete.setDate(1, availability);
            statementDelete.setString(2, CName);
            statementDelete.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error occurred when updating reservation");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    }

    private static void uploadAvailability(String[] tokens) {
        // upload_availability <date>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 2 to include all information (with the operation name)
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        String date = tokens[1];
        try {
            Date d = Date.valueOf(date);
            currentCaregiver.uploadAvailability(d);
            System.out.println("Availability uploaded!");
        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date!");
        } catch (SQLException e) {
            System.out.println("Error occurred when uploading availability");
            e.printStackTrace();
        }
    }

    private static void cancel(String[] tokens) {
        // TODO: Extra credit
        // User must be logged in before starting cancellation.
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please log in first!");
            return;
        }
        // Length of token must be 2.
        if (tokens.length != 2) {
            System.out.println("Wrong input. Please try again!");
            return;
        }
        String appointment = tokens[1];
        if (!appointmentExists(appointment)) {
            return;
        }
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        String selectAppointment = "Select * from Appointment where ID = ?";
        String insertAvailability = "Insert into Availabilities values (? , ?)";
        String cancelAppointment = "Delete from Appointment where ID = ?";
        try {
            PreparedStatement statementSelect = con.prepareStatement(selectAppointment);
            statementSelect.setString(1, appointment);
            ResultSet getResult = statementSelect.executeQuery();
            while (getResult.next()) {
                String CName = getResult.getString("CName");
                Date availability = getResult.getDate("Date");
                PreparedStatement statementInsert = con.prepareStatement(insertAvailability);
                statementInsert.setDate(1, availability);
                statementInsert.setString(2, CName);
                statementInsert.executeUpdate();
                String VName = getResult.getString("VName");
                Vaccine vaccine = new Vaccine.VaccineGetter(VName).get();
                vaccine.increaseAvailableDoses(1);
            }
            PreparedStatement statementCancel = con.prepareStatement(cancelAppointment);
            statementCancel.setString(1, appointment);
            statementCancel.executeUpdate();
            System.out.println("Appointment has been cancelled successfully!");
        } catch (SQLException e) {
            System.out.println("Error occurred when cancelling appointment");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    }

    private static boolean appointmentExists (String appointment) {
        // Todo: Extra credit(method used in cancel)
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        String selectID = "Select * from Appointment where ID = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectID);
            statement.setString(1, appointment);
            ResultSet getResult = statement.executeQuery();
            // Returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            if (!getResult.isBeforeFirst()) {
                System.out.println("Invalid appointment ID. Please try again!");
                return false;
            }
            String userCaregiver = null;
            String userPatient = null;
            while (getResult.next()){
                userCaregiver = getResult.getString("CName");
                userPatient = getResult.getString("PName");
            }
            // Returns false if a patient or a caregiver tries to cancel an appointment that is not correlated with him or her.
            if ((reservingCaregiver != null && !reservingCaregiver.equals(userCaregiver)) || (reservingPatient != null && !reservingPatient.equals(userPatient))) {
                System.out.println("You can't cancel appointments that are not correlated with you.");
                return false;
            }
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static void addDoses(String[] tokens) {
        // add_doses <vaccine> <number>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String vaccineName = tokens[1];
        int doses = Integer.parseInt(tokens[2]);
        Vaccine vaccine = null;
        try {
            vaccine = new Vaccine.VaccineGetter(vaccineName).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when adding doses");
            e.printStackTrace();
        }
        // check 3: if getter returns null, it means that we need to create the vaccine and insert it into the Vaccines
        //          table
        if (vaccine == null) {
            try {
                vaccine = new Vaccine.VaccineBuilder(vaccineName, doses).build();
                vaccine.saveToDB();
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        } else {
            // if the vaccine is not null, meaning that the vaccine already exists in our table
            try {
                vaccine.increaseAvailableDoses(doses);
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        }
        System.out.println("Doses updated!");
    }

    private static void showAppointments(String[] tokens) {
        // TODO: Part 2
        // Must log in before see the appointments.
        if (currentCaregiver == null & currentPatient == null) {
            System.out.println("Please login first!");
            return;
        }
        // Length of token must be 1.
        if (tokens.length != 1) {
            System.out.println("Wrong input. Please try again!");
            return;
        }
        try {
            if (currentPatient != null) {
                String inputSQL = "Select * from Appointment where PName = ?";
                show(reservingPatient, inputSQL);
            }
            if (currentCaregiver != null) {
                String inputSQL = "Select * from Appointment where CName = ?";
                show(reservingCaregiver, inputSQL);
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid operation name!");
        }
    }

    private static void show(String givenName, String inputSQL) {
        // Todo: Part 2 (Method used in showAppointments)
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        try {
            PreparedStatement getAppointment = con.prepareStatement(inputSQL);
            getAppointment.setString(1, givenName);
            ResultSet getResult = getAppointment.executeQuery();
            if (!getResult.isBeforeFirst()) {
                System.out.println("No appointment.");
            }
            while (getResult.next()) {
                System.out.println("Appointment ID: " + getResult.getString("ID"));
                System.out.println("Vaccine name: " + getResult.getString("VName"));
                System.out.println("Date assigned: " + getResult.getString("Date"));
                if (currentCaregiver == null) {
                    System.out.println("Caregiver name: " + getResult.getString("CName"));
                } else {
                    System.out.println("Patient name: " + getResult.getString("PName"));
                }
            }
        } catch(SQLException e) {
            System.out.println("Error occurred when trying to update reservation.");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    }

    private static void logout(String[] tokens) {
        // TODO: Part 2 (Done)
        // Can't log out if haven't logged in.
        if (currentPatient == null && currentCaregiver == null) {
            System.out.println("You can't log out before logging in!");
            return;
        }
        // Length of token must be 1.
        if (tokens.length != 1) {
            System.out.println("Wrong input. Please try again!");
            return;
        }
        currentPatient = null;
        currentCaregiver = null;
        System.out.println("Logged out successfully!");
    }
}