package com.works.restcontrollers;

import com.works.documents.ElasticCustomer;
import com.works.elasticrepositories.ECustomerRepository;
import com.works.entities.Customer;
import com.works.repositories.CustomerRepository;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/customer")
public class CustomerRestController {

    final ElasticsearchOperations elasticsearchOperations;
    final ECustomerRepository eRepo;
    final CustomerRepository cRepo;

    public CustomerRestController(ElasticsearchOperations elasticsearchOperations, ECustomerRepository eRepo, CustomerRepository cRepo) {
        this.elasticsearchOperations = elasticsearchOperations;
        this.eRepo = eRepo;
        this.cRepo = cRepo;
    }


    // add - entity
    @PostMapping("/add")
    public Map<String, Object> add(@RequestBody Customer customer) {
        Map<String, Object> hm = new LinkedHashMap<>();

        Customer c = cRepo.save(customer);
        // Eleastic Search insert
        ElasticCustomer ec = new ElasticCustomer();
        ec.setCid(c.getCid());
        ec.setEmail(c.getEmail());
        ec.setName(c.getName());
        eRepo.save(ec);

        hm.put("status", true);
        hm.put("result", c);

        return hm;
    }


    // list Elastic
    @GetMapping("/list/{page}")
    public Map<String, Object> list(@PathVariable int page) {
        Map<String, Object> hm = new LinkedHashMap<>();

        Iterable<ElasticCustomer> ls = eRepo.findAll();

        // Pageable
        Pageable pageable = PageRequest.of(page, 2);
        Page<ElasticCustomer> lsp = eRepo.findAll(pageable);

        hm.put("status", true);
        hm.put("totalElemets", lsp.getTotalElements());
        hm.put("totalPages", lsp.getTotalPages());
        hm.put("result", lsp.getContent());
        return hm;
    }


    // search data
    @GetMapping("/search/{q}/{page}")
    public Map<String, Object> search(@PathVariable String q, @PathVariable int page) {
        Map<String, Object> hm = new LinkedHashMap<>();

        Pageable pageable = PageRequest.of(page, 2);
        Page<ElasticCustomer> lsp = eRepo.searchEmailAndName(q, pageable);

        hm.put("status", true);
        hm.put("totalElemets", lsp.getTotalElements());
        hm.put("totalPages", lsp.getTotalPages());
        hm.put("result", lsp.getContent());

        return hm;
    }


    @GetMapping("/emailCidSearch/{q}")
    public Map<String, Object> search(@PathVariable String q) {

        Map<String, Object> hm = new LinkedHashMap<>();
        List<ElasticCustomer> ls = eRepo.findByEmailContainsOrCidContains(q, q);
        hm.put("status", true);
        hm.put("result", ls);

        return hm;
    }

    @PostMapping("/globalSearch/{q}")
    public Map<String, Object> globalSearch(@PathVariable String q) {
        Map<String, Object> hm = new LinkedHashMap<>();

        final NativeSearchQuery query =
                new NativeSearchQueryBuilder()
                        .withQuery(
                                QueryBuilders.matchQuery("name", q)
                                        .fuzziness(Fuzziness.AUTO) //daha detayli arama birimi
                                        .prefixLength(2)
                                        .operator(Operator.AND)
                                        .minimumShouldMatch("50")
                        )
                        .build();
        List<SearchHit<ElasticCustomer>> ls = elasticsearchOperations.search(query, ElasticCustomer.class).getSearchHits();
        hm.put("result", ls);
        return hm;
    }

    @DeleteMapping("delete/{cid}")
    public Map<String, Object> delete(@PathVariable String cid) {
        Map<String, Object> hm = new LinkedHashMap<>();

        try {
            int intCid = Integer.parseInt(cid);
            cRepo.deleteById(intCid);
            System.out.println();
            Optional<ElasticCustomer> oCustomer = eRepo.findByCid(cid);

            if (oCustomer.isPresent()) {
                ElasticCustomer elasticCustomer = oCustomer.get();
                eRepo.deleteById(elasticCustomer.getId());
                hm.put("status", true);
                hm.put("result", elasticCustomer.getEmail() + " deleted");
            } else {
                hm.put("status", false);
                hm.put("message", "No such customer");
            }
        } catch (Exception ex) {
            hm.put("status", false);
            hm.put("message", "No id");
        }
        return hm;
    }

    @PutMapping("/update")
    public Map<String, Object> update(@RequestBody Customer customer) {
        Map<String, Object> hm = new LinkedHashMap<>();
        System.out.println(customer.getCid());
        Optional<Customer> opt = cRepo.findById(customer.getCid());
        if(opt.isPresent())
        {
            Customer c = opt.get();
            c.setCid(customer.getCid());
            c.setEmail(customer.getEmail());
            c.setName(customer.getName());
            cRepo.save(c);

            Optional<ElasticCustomer> optE = eRepo.findByCid(customer.getCid().toString());
            if (optE.isPresent()) {

                ElasticCustomer elasticCustomer = optE.get();
                elasticCustomer.setCid(customer.getCid());
                elasticCustomer.setEmail(customer.getEmail());
                elasticCustomer.setName(customer.getName());

                eRepo.deleteById(elasticCustomer.getId());
                eRepo.save(elasticCustomer);

                hm.put("status", true);
                hm.put("result", "Updated " + elasticCustomer.toString());
            }
            else {
                hm.put("status", false);
                hm.put("Message", "No such user in ElasticSearch");
            }
        }
        else {
            hm.put("status", false);
            hm.put("result", "No such User");
        }
        return hm;
    }

}



