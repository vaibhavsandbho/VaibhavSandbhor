package com.ats.lumax.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.ZonedDateTime;

@Data
@Entity
@Table(name = "master_product_variant_details")
public class MasterProductVariantDetails {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "product_variant_id")
	private Integer productVariantId;

	@Column(name = "product_variant_code", length = 50)
	private String productVariantCode;

	@Column(name = "product_variant_name", length = 100)
	private String productVariantName;

	@Column(name = "product_variant_desc", length = 255)
	private String productVariantDesc;

	@Column(name = "capacity")
	private Integer capacity;

	@Column(name = "quantity")
	private Integer quantity;

	@Column(name = "product_id")
	private Integer productId;

	@Column(name = "cdatetime")
	private ZonedDateTime cdatetime;

	@Column(name = "user_id")
	private Integer userId;

	@Column(name = "trolley_type", length = 50)
	private String trolleyType;

	@Column(name = "trolley_storage_capacity")
	private Integer trolleyStorageCapacity;

	@Column(name = "ageing_days")
	private Integer ageingDays;

	@Column(name = "product_varient_is_active")
	private Boolean productVarientIsActive = false;

	@Column(name = "product_variant_is_deleted")
	private Boolean productVariantIsDeleted = false;
}