package com.reservation.application.dao;

import com.reservation.application.entities.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DAO {
    private static String url1;
    private static String user;
    private static String password;

    public static void registerDriver(String url, String usr, String pword) {
        try {
            url1 = url;
            user = usr;
            password = pword;
            DriverManager.registerDriver(new com.mysql.jdbc.Driver());
            System.out.println("Driver correttamente registrato");
        } catch (SQLException e) {
            System.out.println("Errore: " + e.getMessage());
        }
    }

// METHODS TO DEVELOP:
//DONE-getAvailableReservations() → prende tutte le ripetizioni disponibili (serve al guest)
//DONE-bookRequestedReservation(int id_reservationAvailable, int id_user) → preleva grazie all'id passato come primo parametro gli attributi da caricare nella relazione requested, cancella la tupla X dalla relazione available e aggiunge una tupla Y alla relazione requested con l'aggiunta dell'id_user, modificando lo state in *prenotata*
//DONE-setReservationState(int id_reservationRequested, String stateToUpdate) → selezionare una ripetizione dalla tabella di requested, marcarla come disdetta (modificare lo stato in deleted), state è un enum che può valere: *disdetta/completata*
//DONE-getRequestedReservations(int id_user) → prende tutte le ripetizioni della tabella requested dello user passato come parametro, con tutte si intende con qualunque tipo di stato
//DONE-getRequestedReservations() → prende tutte le ripetizioni dalla tabella requested
//DONE-insertCourse(String title)
//DONE-removeCourse(int id_course)
//DONE-insertTeacher(String name, String surname)
//DONE-removeTeacher(int id_teacher)
//DONE-insertUser(String name, String surname)
//DONE-removeUser(int id_course)
//DONE-insertAvailableReservation(int id_teacher, int id_course, String date, String time)
//DONE-removeAvailableReservation(int id_reservationAvailable)
//DONE-getUserRole(String email, String password) lo mette in sessione utente
    //* chiamabili solo dalle sezioni già prenotate
    //da admin e prenotazioni
    private static int checkUserBusyByReservationID(int idReservationRequested, Statement st, String action) throws SQLException{
        String checkForOtherUniqueStatus  =
                String.format(
                        "select COUNT('status') as is_unique " +
                                "from reservation_requested A INNER join reservation_requested B on A.rdate = B.rdate and A.rtime = B.rtime AND B.id = %d " +
                                "where A.id_user = B.id_user and " +
                                "(A.status = 'completed' or A.status= 'booked')",
                        idReservationRequested

                );
        if(action.equals("completed")) {
            checkForOtherUniqueStatus =String.format(
                            "select COUNT('status') as is_unique " +
                                    "from reservation_requested A INNER join reservation_requested B on A.rdate = B.rdate and A.rtime = B.rtime AND A.id = %d AND B.id=A.id " +
                                    "where A.id_user = B.id_user and " +
                                    "(A.status= 'booked')",
                            idReservationRequested

                    );
        }
        ResultSet rs = st.executeQuery(checkForOtherUniqueStatus);
        int completedOrBookedReservations = 0;
        while (rs.next()) {
            completedOrBookedReservations = rs.getInt("is_unique");
        }
        return completedOrBookedReservations;
    }
    //DA PAGINA PRENOTA
    private static int checkUserBusyByReservationIDBooking(int idReservationRequested, Statement st, int id_user) throws SQLException{
        String checkForOtherUniqueStatus =
                String.format(
                        "select COUNT('id') as is_unique " +
                                "from reservation_available A INNER join reservation_requested B on A.date = B.rdate and A.time = B.rtime and B.id_user=%d AND A.id = %d " +
                                "where " +
                                "B.status = 'completed' or B.status= 'booked'",
                        id_user,
                        idReservationRequested

                );
        ResultSet rs = st.executeQuery(checkForOtherUniqueStatus);
        int completedOrBookedReservations = 0;
        while (rs.next()) {
            completedOrBookedReservations = rs.getInt("is_unique");
        }
        return completedOrBookedReservations;
    }
    private static boolean checkTeacherBusyByReservationIDBooking(int idReservationRequested, Statement st) throws SQLException{
        String checkTeacherBusy =
                String.format(
                        "select COUNT('id') as is_unique\n" +
                                "\tfrom reservation_available A INNER join reservation_requested B on A.date = B.rdate and A.time = B.rtime and A.id_teacher=B.id_teacher AND A.id = %d\n" +
                                "\twhere \n" +
                                "    B.status = 'completed' or B.status= 'booked'",
                        idReservationRequested

                );
        ResultSet rs = st.executeQuery(checkTeacherBusy);
        int completedOrBookedReservations = 0;
        while (rs.next()) {
            completedOrBookedReservations = rs.getInt("is_unique");
        }
        if (completedOrBookedReservations > 0)
            return true;
        else
            return false;
    }
    private static int checkTeacherBusyByReservationID(int idReservationRequested, Statement st) throws SQLException{
        String checkTeacherBusy =
                String.format(
                        "select COUNT('status') as is_unique " +
                                "from reservation_requested A INNER join reservation_requested B on A.rdate = B.rdate and A.rtime = B.rtime AND B.id = %d " +
                                "where A.id_teacher = B.id_teacher and " +
                                "(A.status = 'completed' or A.status= 'booked')",
                        idReservationRequested

                );
        ResultSet rs = st.executeQuery(checkTeacherBusy);
        int completedOrBookedReservations = 0;
        while (rs.next()) {
            completedOrBookedReservations = rs.getInt("is_unique");
        }
        return completedOrBookedReservations;
    }    //* chiamabili solo dalle sezioni già prenotate

    public static List<ReservationAvailable> getAvailableReservations() {
        Connection connection = null;
        ArrayList<ReservationAvailable> out = new ArrayList<>();
        try {
            connection = DriverManager.getConnection(url1, user, password);
            if (connection != null) {
                System.out.println("Connected to the database");
            }

            String query = "" +
                    "SELECT reservation_available.id as res_id, t.id as teacher_id, name, surname, c.id as course_id ,title, date, time " +
                    "FROM reservation_available join course c on reservation_available.id_course = c.id join teacher t on t.id = reservation_available.id_teacher "+
                    "ORDER BY date,time asc";

            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery(query);
            while (rs.next()) {

                ReservationAvailable reservationAvailable = new ReservationAvailable(
                        Integer.parseInt(rs.getString("res_id")),
                        new Teacher(Integer.parseInt(rs.getString("teacher_id")), rs.getString("name"), rs.getString("surname")),
                        new Course(Integer.parseInt(rs.getString("course_id")), rs.getString("title")),
                        rs.getString("date"),
                        rs.getString("time")
                );

                out.add(reservationAvailable);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e2) {
                    System.out.println(e2.getMessage());
                }
            }
        }
        return out;
    }

    /**
     * Preleva, grazie all'id passato come primo parametro, gli attributi da caricare nella relazione requested,
     * cancella la tupla X dalla relazione available e aggiunge una tupla Y alla relazione requested con
     * l'aggiunta dell'id_user, modificando lo state in prenotata
     */
    public static void bookRequestedReservation(int id_reservationAvailable, int id_user) throws Exception {
        Connection connection = null;
        connection = DriverManager.getConnection(url1, user, password);
        int count = 0;
        if (connection != null) {
            System.out.println("Connected to the database");
        }

        String queryFromResAvailable = String.format("SELECT `id_teacher`, `id_course`, `date`, `time` FROM `reservation_available` WHERE id = %d;", id_reservationAvailable);
        String queryDeleteResAvailable = String.format("DELETE FROM `reservation_available` WHERE id = %d;", id_reservationAvailable);
        String insertToResRequested = "";
        Statement st = connection.createStatement();
        Statement stDML = connection.createStatement();


        if (checkTeacherBusyByReservationIDBooking(id_reservationAvailable, st)) {
            if (connection != null) {
                connection.close();
            }
            throw new Exception("L'insegnante è già impegnato in una prenotazione per questo giorno a quest'ora");
        }
        if (checkUserBusyByReservationIDBooking(id_reservationAvailable, st, id_user)>0) {
            if (connection != null) {
                connection.close();
            }
            throw new Exception("Sei già impegnato in una prenotazione per questo giorno a quest'ora");
        }


        ResultSet rsReservationAvailable = st.executeQuery(queryFromResAvailable);

        while (rsReservationAvailable.next()) {
            count++;
            insertToResRequested = String.format("INSERT INTO `reservation_requested`(`id_user`, `id_teacher`, `id_course`, `rdate`, `rtime`, `status`) VALUES (%d,%d,%d,'%s','%s','%s')",
                    id_user,
                    Integer.parseInt(rsReservationAvailable.getString("id_teacher")),
                    Integer.parseInt(rsReservationAvailable.getString("id_course")),
                    rsReservationAvailable.getString("date"),
                    rsReservationAvailable.getString("time"),
                    "booked");
        }

        if (count == 0) {
            connection.close();
            throw new SQLException("noresfound");
        }

        if (stDML.executeUpdate(insertToResRequested) != 0)
            System.out.println("La tupla è stata inserita nella tabella reservation requested!");
        else {
            connection.close();
            throw new SQLException();
        }
        if (stDML.executeUpdate(queryDeleteResAvailable) != 0)
            System.out.println("La tupla è stata eliminata dalla tabella reservation available!");

        if (connection != null) {
            connection.close();
        }
    }

    public static boolean checkReservationOwner(int idRequestedReservation, int uID) throws SQLException{
        Connection connection = null;
        int amountOfItemsWithGivenID = 0;

        connection = DriverManager.getConnection(url1, user, password);

        if (connection != null) {
            System.out.println("Connected to the database");
        }

        String query = String.format("select count(id) as ownedItemsWithID " +
                "from reservation_requested " +
                "where reservation_requested.id = %d " +
                "and reservation_requested.id_user = %d",
                idRequestedReservation, uID);

        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(query);
        if (rs.next()) {
            amountOfItemsWithGivenID = rs.getInt("ownedItemsWithID");
        }
        if (connection != null) {
            connection.close();
        }
        if(amountOfItemsWithGivenID >0){
            return true;
        }else{
            return false;
        }
    }

    public static void setReservationState(int idReservationRequested, String stateToUpdate) throws Exception {
        Connection connection = null;
        connection = DriverManager.getConnection(url1, user, password);
        if (connection != null) {
            System.out.println("Connected to the database");
        }
        /*? controlla se l'utente della prenotazione con id x ha altre prenotazioni che hanno lo stato
        *   completato o prenotato -> se l'utente x ha un'altra prenotazione prenotata o cancellata alle ore y, ricavata
        *   dall'id di x allora il conter is_unique sarà maggiore di 0
        */


        String queryUpdateResRequested = String.format("UPDATE reservation_requested SET status = '%s' WHERE id = %d;", stateToUpdate, idReservationRequested);

        Statement st = connection.createStatement();
        //stiamo cambiando lo stato di una prenotazione già fatta.
        if(!stateToUpdate.equals("deleted")) {
            if(stateToUpdate.equals("completed")) {
                int nReservations = checkUserBusyByReservationID(idReservationRequested, st, "completed");
                if (nReservations>1) {
                    if (connection != null) {
                        connection.close();
                    }
                    throw new Exception("L'utente ha già una prenotazione completata per questo giorno a quest'ora");
                }else if(nReservations==0){
                    if (connection != null) {
                        connection.close();
                    }
                    throw new Exception("L'utente non è prenotato a questo evento");
                }
            }else {
                if (checkUserBusyByReservationID(idReservationRequested, st, "booked") > 0) {
                    if (connection != null) {
                        connection.close();
                    }
                    throw new Exception("L'utente ha già una prenotazione attiva per questo giorno a quest'ora");
                }
            }
        }
        if(!stateToUpdate.equals("deleted")) {
            if(stateToUpdate.equals("completed")) {
                if (checkTeacherBusyByReservationID(idReservationRequested, st)>1) {
                    if (connection != null) {
                        connection.close();
                    }
                    throw new Exception("L'insegnante è già impegnato in una prenotazione per questo giorno a quest'ora");
                }
            }else{
                if (checkTeacherBusyByReservationID(idReservationRequested, st)>0) {
                    if (connection != null) {
                        connection.close();
                    }
                    throw new Exception("L'insegnante è già impegnato in una prenotazione per questo giorno a quest'ora");
                }
            }

        }
        if (st.executeUpdate(queryUpdateResRequested) != 0) {
            if (stateToUpdate.equals("deleted")){
                String reinsertBooking = String.format("" +
                                "INSERT INTO reservation_available (id_teacher, id_course, date, time)\n" +
                                "SELECT id_teacher, id_course, rdate, rtime\n" +
                                "FROM reservation_requested\n" +
                                "WHERE reservation_requested.id = %d; ",
                        idReservationRequested
                );
                if (st.executeUpdate(reinsertBooking) != 0)
                    System.out.println("Lo stato della prenotazione è stato correttamente modificato ed è stata reinserita!");
            }else{
                System.out.println("Lo stato della prenotazione è stato correttamente modificato!");
            }
        }else {
            connection.close();
            throw new SQLException();
        }

        if (connection != null) {
            connection.close();
        }

    }

    public static List<ReservationRequested> getRequestedReservations() {
        Connection connection = null;
        ArrayList<ReservationRequested> out = new ArrayList<>();
        try {
            connection = DriverManager.getConnection(url1, user, password);
            if (connection != null) {
                System.out.println("Connected to the database");
            }

            String query = "" +
                    "SELECT reservation_requested.id as res_id, u.id as user_id, email, t.id as teacher_id, name, surname, c.id as course_id ,title, rdate, rtime, status " +
                    "FROM reservation_requested join course c on c.id = reservation_requested.id_course join teacher t on reservation_requested.id_teacher = t.id join user u on reservation_requested.id_user = u.id;";

            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery(query);
            while (rs.next()) {

                ReservationRequested reservationRequested = new ReservationRequested(
                        Integer.parseInt(rs.getString("res_id")),
                        new User(Integer.parseInt(rs.getString("user_id")), rs.getString("email")),
                        new Teacher(Integer.parseInt(rs.getString("teacher_id")), rs.getString("name"), rs.getString("surname")),
                        new Course(Integer.parseInt(rs.getString("course_id")), rs.getString("title")),
                        rs.getString("rdate"),
                        rs.getString("rtime"),
                        rs.getString("status")
                );

                out.add(reservationRequested);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e2) {
                    System.out.println(e2.getMessage());
                }
            }
        }
        return out;
    }

    public static List<User> getUsers() {
        Connection connection = null;
        ArrayList<User> out = new ArrayList<>();
        try {
            connection = DriverManager.getConnection(url1, user, password);
            if (connection != null) {
                System.out.println("Connected to the database");
            }

            String query = "" +
                    "SELECT id as user_id, email " +
                    "FROM user;";

            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery(query);
            while (rs.next()) {

                User user = new User(
                        Integer.parseInt(rs.getString("user_id")),
                        rs.getString("email")

                );

                out.add(user);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e2) {
                    System.out.println(e2.getMessage());
                }
            }
        }
        return out;
    }

    public static List<ReservationRequested> getRequestedReservationsByUserMail(String email) {
        Connection connection = null;
        ArrayList<ReservationRequested> out = new ArrayList<>();
        try {
            connection = DriverManager.getConnection(url1, user, password);
            if (connection != null) {
                System.out.println("Connected to the database");
            }

            String query = String.format(
                    "SELECT reservation_requested.id as res_id, u.id as user_id, email, t.id as teacher_id, name, surname, c.id as course_id ,title, rdate, rtime, status " +
                    "FROM reservation_requested join course c on c.id = reservation_requested.id_course join teacher t on reservation_requested.id_teacher = t.id join user u on reservation_requested.id_user = u.id " +
                    "WHERE email = '%s'" +
                    "ORDER BY rdate,rtime asc;", email);

            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery(query);
            while (rs.next()) {

                ReservationRequested reservationRequested = new ReservationRequested(
                        Integer.parseInt(rs.getString("res_id")),
                        new User(Integer.parseInt(rs.getString("user_id")), rs.getString("email")),
                        new Teacher(Integer.parseInt(rs.getString("teacher_id")), rs.getString("name"), rs.getString("surname")),
                        new Course(Integer.parseInt(rs.getString("course_id")), rs.getString("title")),
                        rs.getString("rdate"),
                        rs.getString("rtime"),
                        rs.getString("status")
                );

                out.add(reservationRequested);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e2) {
                    System.out.println(e2.getMessage());
                }
            }
        }
        return out;
    }

    public static List<Course> getCourses() {
        Connection connection = null;
        ArrayList<Course> out = new ArrayList<>();
        try {
            connection = DriverManager.getConnection(url1, user, password);
            if (connection != null) {
                System.out.println("Connected to the database");
            }

            String query = "SELECT * FROM course;";

            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery(query);
            while (rs.next()) {
                Course course = new Course(
                        Integer.parseInt(rs.getString("id")),
                        rs.getString("title")
                );
                out.add(course);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e2) {
                    System.out.println(e2.getMessage());
                }
            }
        }
        return out;
    }

    public static void insertCourses(String title) throws SQLException{
        Connection connection = null;
        connection = DriverManager.getConnection(url1, user, password);
        if (connection != null) {
            System.out.println("Connected to the database");
        }
        String query = String.format("INSERT INTO course(title) VALUES ('%s')", title);
        Statement st = connection.createStatement();
        if (st.executeUpdate(query) != 0)
            System.out.println(title + " è stato aggiunto al database!");
        else {
            connection.close();
            throw new SQLException("Nessun corso è stato aggiunto");
        }
        if (connection != null) {
                connection.close();
        }
    }

    public static void removeCourses(int courseId) throws SQLException{
        Connection connection = null;
        connection = DriverManager.getConnection(url1, user, password);
        if (connection != null) {
            System.out.println("Connected to the database");
        }
        String query = String.format("DELETE FROM course WHERE id = %d", courseId);
        Statement st = connection.createStatement();
        if (st.executeUpdate(query) != 0)
            System.out.println("Il corso con id = " + courseId + " è stato eliminato dal database!");
        else {
            connection.close();
            throw new SQLException("Nessun corso è stato rimosso");
        }
        if (connection != null) {
                connection.close();
        }
    }

    public static List<Teacher> getTeachers() {
        Connection connection = null;
        ArrayList<Teacher> out = new ArrayList<>();
        try {
            connection = DriverManager.getConnection(url1, user, password);
            if (connection != null) {
                System.out.println("Connected to the database");
            }

            String query = "SELECT * FROM teacher;";

            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery(query);
            while (rs.next()) {
                Teacher teacher = new Teacher(
                        Integer.parseInt(rs.getString("id")),
                        rs.getString("name"),
                        rs.getString("surname")
                );
                out.add(teacher);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e2) {
                    System.out.println(e2.getMessage());
                }
            }
        }
        return out;
    }

    public static void insertTeacher(String name, String surname) throws SQLException {
        Connection connection = null;
        connection = DriverManager.getConnection(url1, user, password);
        if (connection != null) {
            System.out.println("Connected to the database");
        }
        String query = String.format("INSERT INTO `teacher`(`name`, `surname`) VALUES ('%s','%s')", name, surname);
        Statement st = connection.createStatement();
        if (st.executeUpdate(query) != 0)
            System.out.println("Il professore è stato aggiunto al database!");
        else {
            connection.close();
            throw new SQLException("Nessun professore è stato inserito");
        }
        if (connection != null) {
                connection.close();
        }
    }

    public static void removeTeacher(int teacherId) throws SQLException {
        Connection connection = null;
        connection = DriverManager.getConnection(url1, user, password);
        if (connection != null) {
            System.out.println("Connected to the database");
            String query = String.format("DELETE FROM teacher WHERE id = %d", teacherId);
            Statement st = connection.createStatement();
            if (st.executeUpdate(query) != 0)
                System.out.println("Il professore con id = " + teacherId + " è stato rimosso dal database!");
            else {
                connection.close();
                throw new SQLException("Nessun docente è stato rimosso");
            }
            if (connection != null) {
                connection.close();
            }
        }
    }

    public static void insertAvailableReservation(int id_teacher, int id_course, String date, String time) throws SQLException{
        Connection connection = null;
        connection = DriverManager.getConnection(url1, user, password);
        if (connection != null) {
            System.out.println("Connected to the database");
        }
        String query = String.format("INSERT INTO reservation_available (id_teacher, id_course, date, time) VALUES (%d,%d,'%s','%s')", id_teacher, id_course, date, time);
        Statement st = connection.createStatement();
        if (st.executeUpdate(query) != 0)
            System.out.println("La prenotazione è stata aggiunta al database!");
        else {
            connection.close();
            throw new SQLException("Nessuna prenotazione è stata aggiunta");
        }
        if (connection != null)
            connection.close();
    }

    public static void removeAvailableReservation(int idAvailableReservation) throws SQLException {
        Connection connection = null;
        connection = DriverManager.getConnection(url1, user, password);
        if (connection != null) {
            System.out.println("Connected to the database");
        }
        String query = String.format("DELETE FROM reservation_available WHERE id = %d", idAvailableReservation);
        Statement st = connection.createStatement();
        if (st.executeUpdate(query) != 0)
            System.out.println("La prenotazione con id = " + idAvailableReservation + " è stato rimossa dal database!");
        else {
            connection.close();
            throw new SQLException("Nessuna prenotazione è stata rimossa");
        }
        if (connection != null)
            connection.close();
    }

    public static String getUserRole(String email, String pword) throws SQLException {
        Connection connection = null;
        String role = "";
        connection = DriverManager.getConnection(url1, user, password);
        if (connection != null) {
            System.out.println("Connected to the database");
        }

        String query = String.format("SELECT `role` FROM `user` WHERE email = '%s' and password = '%s'", email, pword);

        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(query);
        if (rs.next()) {
            role = rs.getString("role");
        }
        if (connection != null) {
                connection.close();
        }
        return role;
    }
    public static int getUserID(String email, String pword) throws SQLException {
        Connection connection = null;
        int uID = -1;
        connection = DriverManager.getConnection(url1, user, password);
        if (connection != null) {
            System.out.println("Connected to the database");
        }

        String query = String.format("SELECT `id` FROM `user` WHERE email = '%s' and password = '%s'", email, pword);

        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(query);
        if (rs.next()) {
            uID = rs.getInt("id");
        }
        if (connection != null) {
                connection.close();
        }
        return uID;
    }

}
