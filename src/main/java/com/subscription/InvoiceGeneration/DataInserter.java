package com.subscription.InvoiceGeneration;

import com.subscription.InvoiceGeneration.model.Customer;
import com.subscription.InvoiceGeneration.repo.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Service
public class DataInserter { // implements CommandLineRunner {
    @Autowired
    private CustomerRepository customerRepository;

    private static final int BATCH_SIZE = 1000000;
    private static final long TOTAL_RECORDS = 1_000_000_000L;

    private final Random random = new Random();

    private final List<String> firstNames = Arrays.asList(
            "James", "Michael", "John", "Robert", "David", "William", "Richard", "Joseph", "Thomas", "Charles",
            "Christopher", "Daniel", "Matthew", "Anthony", "Mark", "Donald", "Steven", "Paul", "Joshua", "Kenneth",
            "Kevin", "Brian", "George", "Timothy", "Ronald", "Edward", "Jason", "Jeffrey", "Ryan", "Jacob",
            "Gary", "Nicholas", "Eric", "Jonathan", "Stephen", "Larry", "Justin", "Scott", "Brandon", "Benjamin",
            "Samuel", "Gregory", "Alexander", "Frank", "Patrick", "Raymond", "Jack", "Dennis", "Jerry", "Tyler",
            "Mary", "Patricia", "Jennifer", "Linda", "Elizabeth", "Barbara", "Susan", "Jessica", "Sarah", "Karen",
            "Lisa", "Nancy", "Betty", "Margaret", "Sandra", "Ashley", "Kimberly", "Emily", "Donna", "Michelle",
            "Carol", "Amanda", "Dorothy", "Melissa", "Deborah", "Stephanie", "Rebecca", "Sharon", "Laura", "Cynthia",
            "Kathleen", "Amy", "Angela", "Shirley", "Anna", "Brenda", "Pamela", "Emma", "Nicole", "Helen",
            "Samantha", "Katherine", "Christine", "Debra", "Rachel", "Carolyn", "Janet", "Catherine", "Maria", "Heather"
    );

    private final List<String> lastNames = Arrays.asList(
            "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis", "Rodriguez", "Martinez",
            "Hernandez", "Lopez", "Gonzalez", "Wilson", "Anderson", "Thomas", "Taylor", "Moore", "Jackson", "Martin",
            "Lee", "Perez", "Thompson", "White", "Harris", "Sanchez", "Clark", "Ramirez", "Lewis", "Robinson",
            "Walker", "Young", "Allen", "King", "Wright", "Scott", "Torres", "Nguyen", "Hill", "Flores",
            "Green", "Adams", "Nelson", "Baker", "Hall", "Rivera", "Campbell", "Mitchell", "Carter", "Roberts",
            "Gomez", "Phillips", "Evans", "Turner", "Diaz", "Parker", "Cruz", "Edwards", "Collins", "Reyes",
            "Stewart", "Morris", "Morales", "Murphy", "Cook", "Rogers", "Gutierrez", "Ortiz", "Morgan", "Cooper",
            "Peterson", "Bailey", "Reed", "Kelly", "Howard", "Ramos", "Kim", "Cox", "Ward", "Richardson",
            "Watson", "Brooks", "Chavez", "Wood", "James", "Bennett", "Gray", "Mendoza", "Ruiz", "Hughes",
            "Price", "Alvarez", "Castillo", "Sanders", "Patel", "Myers", "Long", "Ross", "Foster", "Jimenez"
    );

    private final List<String> domains = Arrays.asList(
            "gmail.com", "yahoo.com", "hotmail.com", "aol.com", "hotmail.co.uk", "hotmail.fr", "msn.com", "yahoo.fr",
            "wanadoo.fr", "orange.fr", "comcast.net", "yahoo.co.uk", "yahoo.com.br", "yahoo.co.in", "live.com",
            "rediffmail.com", "free.fr", "gmx.de", "web.de", "yandex.ru", "ymail.com", "libero.it", "outlook.com",
            "uol.com.br", "bol.com.br", "mail.ru", "cox.net", "hotmail.it", "sbcglobal.net", "sfr.fr", "live.fr",
            "verizon.net", "live.co.uk", "googlemail.com", "yahoo.es", "ig.com.br", "live.nl", "bigpond.com",
            "terra.com.br", "yahoo.it", "neuf.fr", "yahoo.de", "alice.it", "rocketmail.com", "att.net", "laposte.net",
            "facebook.com", "bellsouth.net", "yahoo.in", "hotmail.es", "charter.net", "yahoo.ca", "yahoo.com.au",
            "rambler.ru", "hotmail.de", "tiscali.it", "shaw.ca", "yahoo.co.jp", "sky.com", "earthlink.net",
            "optonline.net", "freenet.de", "t-online.de", "aliceadsl.fr", "virgilio.it", "home.nl", "qq.com",
            "telenet.be", "me.com", "yahoo.com.ar", "tiscali.co.uk", "yahoo.com.mx", "voila.fr", "gmx.net",
            "mail.com", "planet.nl", "tin.it", "live.it", "ntlworld.com", "arcor.de", "yahoo.co.id", "frontiernet.net",
            "hetnet.nl", "live.com.au", "yahoo.com.sg", "zonnet.nl", "club-internet.fr", "juno.com", "optusnet.com.au",
            "blueyonder.co.uk", "bluewin.ch", "skynet.be", "sympatico.ca", "windstream.net", "mac.com",
            "centurytel.net", "chello.nl", "live.ca", "aim.com", "bigpond.net.au"
    );

    private final List<String> planCodes = Arrays.asList(
            "BASIC", "PRO", "ENTERPRISE", "PREMIUM", "GOLD", "SILVER", "PLATINUM", "ULTIMATE", "STANDARD",
            "ADVANCED", "ELITE", "BUSINESS", "PERSONAL", "FAMILY", "STUDENT", "TRIAL", "FREE", "PLUS", "MAX", "VIP"
    );

    private final List<String> statuses = Arrays.asList(
            "ACTIVE", "INACTIVE", "PENDING", "CANCELLED", "SUSPENDED", "EXPIRED", "TRIAL", "PAUSED", "BLOCKED", "DELETED"
    );

    private final List<String> streets = Arrays.asList(
            "Main St", "2nd St", "3rd St", "1st St", "Oak St", "4th St", "Elm St", "Pine St", "Church St", "Maple St",
            "Walnut St", "5th St", "N Main St", "S Main St", "Washington St", "6th St", "High St", "Chestnut St",
            "Park St", "Cedar St", "Spruce St", "Mill St", "7th St", "W Main St", "E Main St", "River Rd", "Center St",
            "Lake St", "Lincoln St", "Cherry St", "8th St", "Locust St", "Water St", "School St", "Hill St", "Grove St",
            "Franklin St", "9th St", "Jefferson St", "Front St", "North St", "South St", "Hickory St", "Park Ave",
            "Willow St", "Union St", "Ash St", "Ridge Rd", "Meadow Ln", "10th St"
    );

    private final List<String> cities = Arrays.asList(
            "New York", "Los Angeles", "Chicago", "Houston", "Phoenix", "Philadelphia", "San Antonio", "San Diego",
            "Dallas", "San Jose", "Austin", "Jacksonville", "Fort Worth", "Columbus", "Indianapolis", "Charlotte",
            "San Francisco", "Seattle", "Denver", "Oklahoma City", "Nashville", "El Paso", "Washington", "Boston",
            "Las Vegas", "Portland", "Detroit", "Louisville", "Memphis", "Baltimore", "Milwaukee", "Albuquerque",
            "Tucson", "Fresno", "Mesa", "Sacramento", "Atlanta", "Kansas City", "Colorado Springs", "Omaha",
            "Raleigh", "Miami", "Long Beach", "Virginia Beach", "Oakland", "Minneapolis", "Tulsa", "Tampa",
            "Arlington", "New Orleans", "Wichita", "Bakersfield", "Cleveland", "Aurora", "Anaheim", "Honolulu",
            "Santa Ana", "Riverside", "Corpus Christi", "Lexington", "Henderson", "Stockton", "Saint Paul",
            "Cincinnati", "St. Louis", "Pittsburgh", "Greensboro", "Lincoln", "Anchorage", "Plano", "Orlando",
            "Irvine", "Newark", "Durham", "Chula Vista", "Toledo", "Fort Wayne", "St. Petersburg", "Laredo",
            "Jersey City", "Chandler", "Madison", "Lubbock", "Scottsdale", "Reno", "Buffalo", "Gilbert",
            "Glendale", "North Las Vegas", "Winston-Salem", "Chesapeake", "Norfolk", "Fremont", "Garland",
            "Irving", "Hialeah", "Richmond", "Boise", "Spokane"
    );

    private final List<Double> discounts = Arrays.asList(
            0.0, 5.0, 10.0, 15.0, 20.0, 25.0, 30.0, 35.0, 40.0, 45.0, 50.0, 55.0, 60.0, 65.0, 70.0, 75.0
    );

    public void run (String... args) throws Exception {
        long inserted = 0;
        while (inserted < TOTAL_RECORDS) {
            List<Customer> batch = new ArrayList<>(BATCH_SIZE);
            for (int i = 0; i < BATCH_SIZE && inserted < TOTAL_RECORDS; i++, inserted++) {
                Customer customer = new Customer();
                String firstName = firstNames.get(random.nextInt(firstNames.size()));
                String lastName = lastNames.get(random.nextInt(lastNames.size()));
                String name = firstName + " " + lastName;
                customer.setName(name);
                customer.setEmail(firstName.toLowerCase() + "." + lastName.toLowerCase() + "@" + domains.get(random.nextInt(domains.size())));
                customer.setPlanCode(planCodes.get(random.nextInt(planCodes.size())));
                customer.setStartDate(LocalDate.now().minusDays(random.nextInt(365 * 10))); // Random date in last 10 years
                customer.setStatus(statuses.get(random.nextInt(statuses.size())));
                int houseNumber = random.nextInt(9999) + 1;
                String street = streets.get(random.nextInt(streets.size()));
                String city = cities.get(random.nextInt(cities.size()));
                customer.setBillingAddress(houseNumber + " " + street + ", " + city + ", USA");
                customer.setDiscount (discounts.get(random.nextInt(discounts.size())));
                customer.setLastBilledMonth(LocalDate.now().minusMonths(random.nextInt(24)).format(DateTimeFormatter.ofPattern("yyyy-MM")));
                batch.add(customer);
            }
            customerRepository.saveAll (batch);
            customerRepository.flush();
            System.out.println ("Inserted " + inserted + " records so far ..");
        }
        System.out.println ("Insertion completed....");
    }
}
