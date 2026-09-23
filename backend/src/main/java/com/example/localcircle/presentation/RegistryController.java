package com.example.localcircle.presentation;

import com.example.localcircle.business.model.Domain.*;
import com.example.localcircle.business.model.PersonalPreference;
import com.example.localcircle.business.service.PersonalPreferenceService;
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

  private final PersonalPreferenceService personal;

  public RegistryController(RegistryService service, PersonalPreferenceService personal) {
    this.service = service;
    this.personal = personal;
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

  public record AccountNumberResponse(String accountNumber) {
    @Override
    public String toString() {
      return "AccountNumberResponse[redacted]";
    }
  }

  @GetMapping("/accounts/{id}/number")
  public ResponseEntity<AccountNumberResponse> accountNumber(
      @RequestAttribute Actor actor, @PathVariable @Positive long id) {
    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .header("Pragma", "no-cache")
        .body(new AccountNumberResponse(personal.accountNumber(actor, id)));
  }

  @GetMapping("/preferences")
  public List<PersonalPreference.View> list(@RequestAttribute Actor actor) {
    return personal.list(actor);
  }

  @GetMapping("/preferences/{id}")
  public PersonalPreference.View get(
      @RequestAttribute Actor actor, @PathVariable @Positive long id) {
    return personal.get(actor, id);
  }

  @PostMapping("/preferences")
  public ResponseEntity<PersonalPreference.View> save(
      @RequestAttribute Actor actor, @Valid @RequestBody PersonalPreference.Input r) {
    var p = personal.save(actor, r);
    return ResponseEntity.created(URI.create("/api/v1/preferences/" + p.preferenceId())).body(p);
  }

  @PutMapping("/preferences/{id}")
  public PersonalPreference.View update(
      @RequestAttribute Actor actor,
      @PathVariable @Positive long id,
      @Valid @RequestBody PersonalPreference.Input r) {
    return personal.update(actor, id, r);
  }

  @DeleteMapping("/preferences/{id}")
  public ResponseEntity<Void> delete(
      @RequestAttribute Actor actor,
      @PathVariable @Positive long id,
      @RequestParam @PositiveOrZero long version) {
    personal.delete(actor, id, version);
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
