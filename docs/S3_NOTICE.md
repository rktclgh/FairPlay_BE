## 🟦 S3 구조 & 규칙 (실서버 공통)

- 버킷명: 
- S3 업로드/다운로드, Spring 코드에서 **AWS SDK v2** `software.amazon.awssdk:s3` 사용
- 버킷 접근은 무조건 `S3Client` 객체,
  리전: `ap-northeast-2`,
  키는 하드코딩 금지, “폴더 구조/네이밍 규칙” 반드시 지켜주새요!

### 🟦 폴더(Key) 규칙 – 무조건 하위폴더로 분리할 것

> root(최상위)나 /uploads에 파일 몰빵 금지.
>
>
> 각자 도메인별/목적별 폴더 강제 분리.
>

예시:

- `uploads/profile/{userId}/{filename}`
- `uploads/event/{eventId}/{filename}`
- `uploads/tmp/{yyyyMMdd}/{filename}`
- `uploads/invite/{token}/{filename}`

### 🟦 코드 예시 (Spring Service)

```java
S3Client s3 = S3Client.builder()
    .region(Region.AP_NORTHEAST_2)
    .build();

String bucket = "버킷명";
String key = "uploads/profile/" + userId + "/" + fileName;

// 업로드
s3.putObject(
    PutObjectRequest.builder().bucket(bucket).key(key).build(),
    RequestBody.fromFile(Paths.get(localPath))
);
```