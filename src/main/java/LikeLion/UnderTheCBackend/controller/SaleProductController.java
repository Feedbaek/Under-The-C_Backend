package LikeLion.UnderTheCBackend.controller;

import LikeLion.UnderTheCBackend.entity.*;
import LikeLion.UnderTheCBackend.repository.ProductRepository;
import LikeLion.UnderTheCBackend.repository.UserRepository;
import LikeLion.UnderTheCBackend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

import static LikeLion.UnderTheCBackend.entity.Role.BUYER;


@RestController
@Slf4j
@Tag(name = "SaleProduct API", description = "상품 판매 API")
@RequestMapping("/api/v1/sale_product")

public class  SaleProductController {
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    private final UserService userService;
    private final String imagesPath = "/src/main/resources/images/";

    @Autowired
    SaleProductController(ProductRepository productRepository, UserRepository userRepository, UserService userService) {
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.userService = userService;
    }
    @PostMapping(value="/add",consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "판매 상품 추가", description = "Product 테이블에 상품 정보 추가", responses = {
            @ApiResponse(responseCode = "200", description = "성공")
    })
    public Product addByProductName(
            @RequestParam String name,
            @RequestParam(required = false) String subTitle,
            @RequestParam(required = false) BigDecimal price,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String subDescription,
            @RequestParam(required = false) MultipartFile mainImage,
            @RequestParam(required = false) List<String> keyword,
            @RequestParam(required = false) List<String> detailImage,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date saleStartDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date saleEndDate,
            @RequestParam(required = false) String category,
            HttpServletRequest request
    ) throws IOException {
        //로그인 정보 왜래키로 받아 Product테이블의 seller_id에 set 필요
        //로그인 정보 예외처리 필요

        // 로그인한 유저인지 확인
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "로그인 되어 있지 않습니다.");
        }
        Long sellerId = (Long) session.getAttribute("user");
        User user = userRepository.findById(sellerId).orElse(null);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "로그인 되어 있지 않습니다.");
        }

        //같은 이름의 상품은 생성 불가
        Optional<Product> existingProduct = productRepository.findByName(name);
        if (existingProduct.isPresent()) {
            // 이미 해당 이름의 상품이 존재하는 경우 예외 처리
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "같은 이름의 상품이 이미 존재합니다.");
        }
        Product newProduct = new Product();
        newProduct.setUserId(user);
        newProduct.setName(name);
        newProduct.setSubTitle(subTitle);
        newProduct.setPrice(price);
        newProduct.setDescription(description);
        newProduct.setSubDescription(subDescription);
//        newProduct.setMain_image(mainImage);
        List<ProductKeywordConnect> keywordEntities = new ArrayList<>();
        for (String keywordStr : keyword) {
            ProductKeywordConnect keywords = new ProductKeywordConnect();

            keywords.setKeyword(keywordStr);
            keywordEntities.add(keywords);
        }
        newProduct.setKeywords(keywordEntities);

        //메인이미지 파일 저장
        if (mainImage != null && !mainImage.isEmpty()) {
            String filename = mainImage.getOriginalFilename();
            log.info("mainImage.getOriginalFilename = {}", filename);

            String absolutePath = System.getProperty("user.dir");;
            log.info(" absolutePath = {}", absolutePath);
            File Folder = new File(absolutePath+imagesPath);
            // 해당 디렉토리가 없을경우 디렉토리를 생성합니다.
            if (!Folder.exists()) {
                try{
                    Folder.mkdirs(); //폴더 생성합니다.
                    System.out.println("폴더가 생성되었습니다.");
                }
                catch(Exception e){
                    e.getStackTrace();
                }
            }else {
                System.out.println("이미 폴더가 생성되어 있습니다.");
            }
            mainImage.transferTo(new File(absolutePath +imagesPath+filename));
            newProduct.setMainImage(filename);
        }

        // 이미지 URL 리스트에서 ReviewImage 엔티티를 생성합니다.
        List<ProductDetailImage> detailImages = new ArrayList<>();
        for (String imageUrl : detailImage) {
            ProductDetailImage productDetailImage = new ProductDetailImage();
            productDetailImage.setImageUrl(imageUrl);
            detailImages.add(productDetailImage);
        }
        newProduct.setDetailImage(detailImages);
        newProduct.setSaleStartDate(saleStartDate);
        newProduct.setSaleEndDate(saleEndDate);
        newProduct.setCategory(category);
        newProduct.setViewCount(0); // 초기 viewCount 설정
        newProduct.setCreatedAt(new Date()); // 현재 시간 설정

        productRepository.save(newProduct);
        return newProduct;
    }

    @GetMapping("/view") //조회수
    @Operation(summary = "판매 상품 찾기", description = "Product 테이블의 id로 특정 상품 반환", responses = {
            @ApiResponse(responseCode = "200", description = "성공")
    })
    @Transactional
    public Optional<Product> findById(@RequestParam("id") Long productId) {
        Optional<Product> product = productRepository.findById(productId);

        if (product.isPresent()) {
            Product existingProduct = product.get();
            existingProduct.setViewCount(existingProduct.getViewCount() + 1); // viewCount를 1 더해줌
            productRepository.save(existingProduct); // 변경된 상품 정보 저장
        }


        return product;
    }
    @GetMapping("/view_all")
    @Operation(summary = "판매 상품 모두 찾기", description = "Product 테이블의 모든 상품 반환", responses = {
            @ApiResponse(responseCode = "200", description = "성공")
    })@Transactional
    public List<Product> findAll(){
        List<Product> product = null;
        product = productRepository.findAll();
        return product;
    }


    @PatchMapping("/update/{id}")
    @Operation(summary = "판매 상품 수정", description = "product table의 id 입력받아 판매 상품 수정", responses = {
            @ApiResponse(responseCode = "200", description = "성공")
    })
    public Product updateProduct(
            @PathVariable("id") Long productId,
            @RequestParam Long sellerId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String subTitle,
            @RequestParam(required = false) BigDecimal price,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String subDescription,
            @RequestParam(required = false) MultipartFile mainImage,
            @RequestParam(required = false) List<String> keyword,
            @RequestParam(required = false) List<String> detailImage,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date saleStartDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date saleEndDate,
            @RequestParam(required = false) String category
    ) {

        Optional<Product> optionalProduct = productRepository.findById(productId);
        if (optionalProduct.isPresent()) {
            Product product = optionalProduct.get();

            // 판매 상품 객체에 productRequest로부터 값을 업데이트합니다.
            if (name != null) {
                Optional<Product> existingProduct = productRepository.findByName(name);
                if (existingProduct.isPresent() && !product.getId().equals(existingProduct.get().getId())) {
                    // 이미 해당 이름의 상품이 존재하는 경우 예외 처리
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "같은 이름의 상품이 이미 존재합니다.");
                }
                product.setName(name);
            }
            if (category != null)
                product.setCategory(category);
            if (price != null)
                product.setPrice(price);
            if (keyword != null) {
                List<ProductKeywordConnect> updatedKeywords = new ArrayList<>();
                for (String keywordStr : keyword) {
                    ProductKeywordConnect keywordEntity = new ProductKeywordConnect();
                    keywordEntity.setKeyword(keywordStr);
                    updatedKeywords.add(keywordEntity);
                }
                product.setKeywords(updatedKeywords);
            }
            if (detailImage != null) {
                List<ProductDetailImage> updatedDetailImages = new ArrayList<>();
                for (String imageUrl : detailImage) {
                    ProductDetailImage productDetailImage = new ProductDetailImage();
                    productDetailImage.setImageUrl(imageUrl);
                    updatedDetailImages.add(productDetailImage);
                }
                product.setDetailImage(updatedDetailImages);
            }
            if (description != null)
                product.setDescription(description);
            if (saleStartDate != null)
                product.setSaleStartDate(saleStartDate);
            if (saleEndDate != null)
                product.setSaleEndDate(saleEndDate);
            if (subTitle != null)
                product.setSubTitle(subTitle);
            if (subDescription != null)
                product.setSubDescription(subDescription);/*
            if (mainImage != null) //이미지 관련 DB접근은 수정 필요함
                product.setMain_image(mainImage);*/
            productRepository.save(product);
            return product;
        }else {
            //예외처리
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "해당 ID의 상품을 찾을 수 없습니다.");
        }
    }
    @DeleteMapping("/delete/{id}")
    @Operation(summary = "판매 상품 삭제", description = "Product 테이블에 지정된 id로 판매 상품 삭제", responses = {
            @ApiResponse(responseCode = "200", description = "성공")
    })
    public Product deleteById(@RequestParam("productId") Long productId) {

        //String sellerId = seller.getId(); seller테이블 받으면 구현 재개

        Optional<Product> product = productRepository.findById(productId);

        if (product.isPresent()) {/* seller테이블 받으면 구현 재개
                if (!product.get().getSeller_id().equals(sellerId)){
                    throw new IllegalArgumentException("해당 상품의 판매자만 삭제할 수 있습니다.");
                }*/
            productRepository.deleteById(productId);
            return product.get();
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "해당 ID의 상품을 찾을 수 없습니다.");
        }
    }

    @PostConstruct
    public void init() {

        for (int i=1; i<=10; ++i) {
            userService.createUser("user"+i, "010-1234-5678"+i, "user"+i, "서울시 강남구"+i, "서울시 강남구"+i, BUYER, "certificate"+i);
            Product product = new Product();
            User user = userRepository.findById((long) i).orElse(null);
            product.setUserId(user);
            product.setName("상품" + i);
            product.setSubTitle("소제목" + i);
            product.setPrice(new BigDecimal(1000 + i));
            product.setDescription("설명" + i);
            product.setSubDescription("추가설명" + i);
            product.setMainImage("이미지" + i);
            product.setSaleStartDate(new Date());
            product.setSaleEndDate(new Date());
            product.setCategory("카테고리" + i);
            product.setViewCount(0);
            product.setCreatedAt(new Date());

            ProductKeywordConnect productKeywords = new ProductKeywordConnect();
            ProductKeyword productKeyword = new ProductKeyword();
            productKeyword.setKeyword("키워드" + i);

            productKeywords.setProductKeyword(productKeyword);
            productKeywords.setProduct(product);

            productRepository.save(product);
        }
    }
}

