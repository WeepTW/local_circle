package com.example.localcircle.presentation;

import com.example.localcircle.business.model.Domain.*;
import com.example.localcircle.business.service.RegistryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class RegistryController {
  private final RegistryService service;

  public RegistryController(RegistryService service) {
    this.service = service;
  }

  @GetMapping("/me")
  public Actor me(@RequestAttribute Actor actor) {
    return actor;
  }

  @GetMapping("/products")
  public List<Product> products() {
    return service.products();
  }

  @GetMapping("/admin/products")
  public List<Product> adminProducts(@RequestAttribute Actor actor) {
    return service.adminProducts(actor);
  }

  @GetMapping("/accounts")
  public List<Account> accounts(@RequestAttribute Actor actor) {
    return service.accounts(actor);
  }

  @GetMapping("/preferences")
  public List<Preference> list(@RequestAttribute Actor actor) {
    return service.preferences(actor);
  }

  @GetMapping("/preferences/{id}")
  public Preference get(@RequestAttribute Actor actor, @PathVariable @Positive long id) {
    return service.preference(actor, id);
  }

  @PostMapping("/preferences")
  public ResponseEntity<Preference> save(
      @RequestAttribute Actor actor, @Valid @RequestBody Requests.Save r) {
    var p = service.save(actor, new SaveCommand(r.productId(), r.accountId(), r.plannedQuantity()));
    return ResponseEntity.created(URI.create("/api/v1/preferences/" + p.preferenceId())).body(p);
  }

  @PutMapping("/preferences/{id}")
  public Preference update(
      @RequestAttribute Actor actor,
      @PathVariable @Positive long id,
      @Valid @RequestBody Requests.Update r) {
    return service.update(
        actor, id, r.version(), new SaveCommand(r.productId(), r.accountId(), r.plannedQuantity()));
  }

  @DeleteMapping("/preferences/{id}")
  public ResponseEntity<Void> delete(
      @RequestAttribute Actor actor,
      @PathVariable @Positive long id,
      @RequestParam @PositiveOrZero long version) {
    service.delete(actor, id, version);
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/admin/products/{id}")
  public Product product(
      @RequestAttribute Actor actor,
      @PathVariable @Positive long id,
      @Valid @RequestBody Requests.ProductUpdate r) {
    return service.updateProduct(
        actor,
        id,
        new ProductCommand(r.productName(), r.price(), r.feeRate(), r.active(), r.version()));
  }
}
