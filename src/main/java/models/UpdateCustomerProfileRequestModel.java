package models;

import generators.GeneratingRule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdateCustomerProfileRequestModel extends BaseModel {
    @GeneratingRule(regex = "^[A-Za-z]{3,10} [A-Za-z]{3,10}$")
    private String name;
}
