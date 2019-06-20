package be.ucll.da.dentravak.controllers;



import be.ucll.da.dentravak.model.*;
import be.ucll.da.dentravak.repositories.SandwichOrderRepository;
import be.ucll.da.dentravak.repositories.SandwichRepository;
import com.opencsv.CSVWriter;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.inject.Inject;
import javax.naming.ServiceUnavailableException;
import javax.servlet.http.HttpServletResponse;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.*;

import static java.util.Map.Entry.comparingByValue;

@RestController
@CrossOrigin(origins = "*")
public class SandwichController {

    @Inject
    private DiscoveryClient discoveryClient;

    @Inject
    private SandwichRepository repository;

    @Inject
    SandwichOrderRepository orderRepository;

    @Inject
    private RestTemplate restTemplate;

    @RequestMapping("/sandwiches")
    public Iterable<Sandwich> sandwiches() {
        try {
            SandwichPreferences preferences = getPreferences("denis");


            Iterable<Sandwich> allSandwiches = sortSandwiches(preferences);
            return allSandwiches;
        } catch (Exception e) {
            System.out.println(e.getCause());
            System.out.println(e.getMessage());
            return repository.findAll();
        }
    }

    private Iterable<Sandwich> sortSandwiches(SandwichPreferences preferences)
    {
            // Create a list from elements of HashMap
            List<Map.Entry<UUID, Float> > list =
                    new LinkedList<>(preferences.entrySet());

            // Sort the list
            Collections.sort(list, new Comparator<Map.Entry<UUID, Float> >() {
                public int compare(Map.Entry<UUID, Float> o1,
                                   Map.Entry<UUID, Float> o2)
                {
                    return (o1.getValue()).compareTo(o2.getValue());
                }
            });

            // put data from sorted list to hashmap
            HashMap<UUID, Float> temp = new LinkedHashMap<UUID, Float>();
            for (Map.Entry<UUID, Float> aa : list) {
                temp.put(aa.getKey(), aa.getValue());
            }
        //find the matching sandwich name from uuid
        List<Sandwich> sortedSandwiches = new ArrayList<>();

        for (Map.Entry<UUID, Float> entry: temp.entrySet()) {
            sortedSandwiches.add(repository.findById(UUID.fromString(entry.getKey().toString())).get());
        }
        //for testing
        for(Sandwich sandwich : sortedSandwiches)
        {
            System.out.println(sandwich.getName());
        }
        //catch all the other non rated sandwiches
        for (Sandwich x : repository.findAll()){
            if (!sortedSandwiches.contains(x))
                sortedSandwiches.add(x);
        }
            return sortedSandwiches;


        }

    @RequestMapping(value = "/sandwiches", method = RequestMethod.POST)
    public Sandwich createSandwich(@RequestBody Sandwich sandwich) {
        return repository.save(sandwich);
    }

    @RequestMapping(value = "/sandwiches/{id}", method = RequestMethod.PUT)
    public Sandwich updateSandwich(@PathVariable UUID id, @RequestBody Sandwich sandwich) {
        if(!id.equals(sandwich.getId())) throw new IllegalArgumentException("Nownow, are you trying to hack us.");
        return repository.save(sandwich);
    }

    // why comment: for testing
    @GetMapping("/getpreferences/{emailAddress}")
    public SandwichPreferences getPreferences(@PathVariable String emailAddress) throws RestClientException, ServiceUnavailableException {
        URI service = recommendationServiceUrl()
                .map(s -> s.resolve("/recommendation/recommend/" + emailAddress))
                .orElseThrow(ServiceUnavailableException::new);
        return restTemplate
                .getForEntity(service, SandwichPreferences.class)
                .getBody();
    }

//    public Optional<URI> recommendationServiceUrl() {
//        try {
//            return Optional.of(new URI("http://localhost:8081"));
//        } catch (URISyntaxException e) {
//            throw new RuntimeException(e);
//        }
//    }

    public Optional<URI> recommendationServiceUrl() {
        return discoveryClient.getInstances("recommendation")
                .stream()
                .map(si -> si.getUri())
                .findFirst();
    }

    @RequestMapping("/orders")
    public Iterable<SandwichOrder> orders() {
        return orderRepository.findAll();
    }

    @RequestMapping(value = "/orders", method = RequestMethod.POST)
    public SandwichOrder createSandwichOrder(@RequestBody SandwichOrder sandwichOrder) {
        sandwichOrder.setCreationDate(LocalDateTime.now());
        return orderRepository.save(sandwichOrder);
    }

    @RequestMapping(value = "/**/**",method = RequestMethod.OPTIONS)
    public ResponseEntity handle() {
        return new ResponseEntity(HttpStatus.OK);
    }


    @RequestMapping("/print")
    public void printToCsv(HttpServletResponse response) {
        List<SandwichOrder> toPrint = orderRepository.findByPrintedFalse();
        for (SandwichOrder order : toPrint)
        {
            //System.out.println(order.getId());
            order.setPrinted(true);
            orderRepository.save(order);
        }

        //set file name and content type
        String filename = "orders.csv";

        response.setContentType("text/csv");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                           "attachment; filename=\"" + filename + "\"");

        try
        {
        StatefulBeanToCsv<SandwichOrder> writer = new StatefulBeanToCsvBuilder<SandwichOrder>(response.getWriter())
                .withQuotechar(CSVWriter.NO_QUOTE_CHARACTER)
                .withSeparator(CSVWriter.DEFAULT_SEPARATOR)
                .withOrderedResults(false)
                .build();
            writer.write(toPrint);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

        //return orderRepository.findAll();
    }

}

