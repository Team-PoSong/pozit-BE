# POZIT 여행 코스 추천 시스템 구현 문서

## 1. 구현 범위

현재 추천 시스템은 MVP 단계로 구현되어 있다.

구현된 기능은 다음과 같다.

- 여행 정보 기반 추천 미리보기 API
- 국문 관광정보 API 키워드 검색 기반 장소 후보 생성
- 장소 후보 기본 필터링
- 사용자 태그 8차원 벡터 생성
- 장소 콘텐츠 8차원 벡터 생성
- 코사인 유사도 기반 콘텐츠 점수 계산
- 선택 태그 균형 점수 반영
- 교통수단 적합도 점수 반영
- 장소 정보 품질 점수 반영
- 다양성 재정렬
- 여행스타일별 날짜별 장소 수 배분
- 좌표 기반 간단한 방문 순서 생성

아직 실제 외부 API 연동이 구현되지 않은 항목은 중립 점수 또는 확장 포인트로 처리한다.

- 관광지 집중률 점수: 현재 중립값 `0.5`
- 지역별 방문자수 점수: 현재 중립값 `0.5`
- 두루누비 코스: 아직 미연동
- 실제 이동시간/환승 수: 아직 미연동
- 추천 결과 확정 저장 API: 아직 미구현

## 2. API

### 여행 코스 추천 미리보기

```http
POST /api/travels/{travelId}/recommendations/preview
```

해당 여행의 조건을 이용해 추천 코스를 생성한다.

이 API는 DB에 추천 결과를 저장하지 않는다. 관광정보 API 응답은 요청 처리 중 메모리에서만 사용한다.

### 권한

요청 사용자가 해당 여행의 멤버여야 한다.

권한 검증은 `TravelMemberRepository.existsByTravelAndUser()`를 사용한다.

### 응답 예시

```json
{
  "isSuccess": true,
  "code": "COMMON200",
  "message": "요청에 성공했습니다.",
  "result": {
    "travelId": 1,
    "dayCount": 2,
    "days": [
      {
        "dayNumber": 1,
        "date": "2026-08-01",
        "places": [
          {
            "orderIndex": 1,
            "contentId": "126508",
            "contentTypeId": "12",
            "title": "경복궁",
            "address": "서울특별시 종로구 사직로 161",
            "imageUrl": "https://...",
            "latitude": 37.579617,
            "longitude": 126.977041,
            "stayMinutes": 90,
            "finalScore": 0.684,
            "contentScore": 0.719,
            "transportationScore": 0.7,
            "qualityScore": 0.9
          }
        ]
      }
    ]
  }
}
```

## 3. 패키지 구조

```text
src/main/java/com/pozit/pozitserver/recommendation
├─ controller
│  └─ CourseRecommendationController
├─ dto
│  └─ RecommendedCourseResponse
├─ model
│  ├─ CandidatePlace
│  ├─ CourseRecommendCommand
│  ├─ PlaceFeatureVector
│  ├─ RecommendationTag
│  └─ ScoredPlace
└─ service
   ├─ ContentScoreCalculator
   ├─ ContextualRankingService
   ├─ CosineSimilarityCalculator
   ├─ CourseRecommendationService
   ├─ DiversityRerankingService
   ├─ PlaceFeatureExtractor
   ├─ PlaceQualityScoreCalculator
   ├─ RouteOptimizationService
   ├─ StayTimePolicy
   ├─ TourApiCandidateProvider
   ├─ TransportationScoreCalculator
   └─ UserPreferenceVectorFactory
```

## 4. 전체 처리 흐름

추천 미리보기 API가 호출되면 다음 순서로 처리된다.

```text
travelId 수신
  ↓
Travel 조회
  ↓
여행 멤버 권한 검증
  ↓
TravelTag 조회
  ↓
CourseRecommendCommand 생성
  ↓
TourApiCandidateProvider로 후보 장소 조회
  ↓
UserPreferenceVectorFactory로 사용자 벡터 생성
  ↓
PlaceFeatureExtractor로 장소 벡터 생성
  ↓
ContentScoreCalculator로 콘텐츠 점수 계산
  ↓
ContextualRankingService로 최종 장소 점수 계산
  ↓
DiversityRerankingService로 반복 카테고리 완화
  ↓
RouteOptimizationService로 날짜별 장소 배치 및 방문 순서 생성
  ↓
RecommendedCourseResponse 반환
```

## 5. 사용자 태그 벡터

추천 태그는 `RecommendationTag` enum으로 관리한다.

벡터 차원은 다음 순서로 고정한다.

```text
0 RECORD      기록
1 FOOD        미식
2 HEALING     힐링
3 EXPERIENCE  체험
4 CULTURE     문화
5 ART         예술
6 SHOPPING    쇼핑
7 EXPLORATION 탐험
```

사용자가 선택한 태그는 `1.0`, 선택하지 않은 태그는 `0.0`으로 둔다.

예를 들어 `미식 + 문화`인 경우:

```text
[0, 1, 0, 0, 1, 0, 0, 0]
```

현재 DB의 `tags.name`은 한국어 값이다.

```sql
기록, 미식, 힐링, 체험, 문화, 예술, 쇼핑, 탐험
```

`RecommendationTag.fromName()`은 한국어 이름과 enum 이름을 모두 인식한다.

## 6. 장소 후보 생성

구현 클래스:

```text
TourApiCandidateProvider
```

현재는 기존 `TourApiClient.searchPlaces()`를 재사용한다.

여행 목적지와 태그에 따라 키워드를 만들어 여러 번 검색한다.

예를 들어 destination이 `서울특별시`, 태그가 `미식 + 문화`이면 다음과 같은 키워드가 사용된다.

```text
서울특별시
서울특별시 맛집
서울특별시 문화
```

태그별 키워드 매핑은 다음과 같다.

| 태그 | 검색 키워드 |
| --- | --- |
| 기록 | `{destination} 관광지` |
| 미식 | `{destination} 맛집` |
| 힐링 | `{destination} 공원` |
| 체험 | `{destination} 체험` |
| 문화 | `{destination} 문화` |
| 예술 | `{destination} 미술관` |
| 쇼핑 | `{destination} 시장` |
| 탐험 | `{destination} 트레킹` |

### 후보 필터링

다음 조건에 해당하는 후보는 제외한다.

- `contentId`가 없는 장소
- 제목이 없는 장소
- 좌표가 없는 장소
- 선택 지역 코드와 맞지 않는 장소
- 동일 `contentId` 중복 장소

지역 필터링은 관광정보 API 응답의 법정동 코드를 사용한다.

- 광역 지역: `lDongRegnCd`
- 시군구 지역: `lDongSignguCd`

`Travel.regionCode`가 `11000`처럼 끝 세 자리가 `000`이면 광역 단위로 판단한다.

## 7. 장소 콘텐츠 벡터 생성

구현 클래스:

```text
PlaceFeatureExtractor
```

현재 장소 벡터는 `contentTypeId`와 장소명 키워드를 조합해 생성한다.

상세 소개정보 API는 아직 호출하지 않는다.

### contentTypeId 기반 점수

| contentTypeId | 의미 | 주요 점수 |
| --- | --- | --- |
| 12 | 관광지 | 기록, 힐링, 문화, 탐험 |
| 14 | 문화시설 | 문화, 예술, 기록 |
| 15 | 행사/축제 | 체험, 문화, 예술 |
| 28 | 레포츠 | 체험, 탐험, 힐링 |
| 38 | 쇼핑 | 쇼핑, 미식, 문화 |
| 39 | 음식점 | 미식, 체험 |

### 제목 키워드 보정

장소명에 특정 키워드가 포함되면 관련 태그 점수를 추가한다.

예시:

| 키워드 | 보정 태그 |
| --- | --- |
| 미술관, 갤러리, 전시, 공연, 극장 | 예술 |
| 시장, 먹거리, 맛집, 식당, 카페, 거리 | 미식 |
| 공원, 숲, 수목원, 휴양림, 정원 | 힐링 |
| 산, 동굴, 섬, 트레킹, 둘레길, 해변 | 탐험 |
| 체험, 공방, 만들기, 레포츠 | 체험 |
| 궁, 유적, 역사, 박물관, 문화 | 문화, 기록 |
| 쇼핑, 몰, 상가, 백화점 | 쇼핑 |

모든 태그 점수는 최대 `1.0`으로 제한한다.

## 8. 콘텐츠 점수

구현 클래스:

```text
ContentScoreCalculator
CosineSimilarityCalculator
```

콘텐츠 점수는 코사인 유사도와 태그 균형 점수를 결합한다.

```text
ContentScore = CosineSimilarity * 0.85 + TagBalanceScore * 0.15
```

`TagBalanceScore`는 선택된 태그들의 장소 점수 중 최솟값이다.

```text
TagBalanceScore = min(선택 태그별 장소 점수)
```

이 점수를 넣은 이유는 한 태그만 강하게 만족하는 장소보다 두 태그를 균형 있게 만족하는 장소를 우선하기 위해서다.

## 9. 최종 장소 점수

구현 클래스:

```text
ContextualRankingService
TransportationScoreCalculator
PlaceQualityScoreCalculator
```

현재 최종 점수 산식은 다음과 같다.

```text
FinalPlaceScore
= ContentScore * 0.50
+ CongestionScore * 0.20
+ TransportationScore * 0.15
+ RegionTrendScore * 0.10
+ QualityScore * 0.05
```

현재 `CongestionScore`와 `RegionTrendScore`는 외부 API 미연동 상태이므로 각각 `0.5`를 사용한다.

### 교통수단 점수

현재 교통수단 점수는 MVP용 단순 규칙이다.

| 교통수단 | 규칙 |
| --- | --- |
| WALK | 좌표가 있으면 기본 가점, 레포츠는 추가 가점 |
| CAR | 주소가 있으면 기본 가점, 음식점은 중간 점수 |
| PUBLIC | 주소와 좌표가 있으면 기본 가점 |

현재 enum은 기존 코드에 맞춰 다음 값을 사용한다.

```text
CAR
WALK
PUBLIC
```

기획서의 `WALKING`, `PUBLIC_TRANSIT`과 이름이 다르므로 API 계약을 바꾸려면 별도 마이그레이션이 필요하다.

### 품질 점수

현재 품질 점수는 다음 항목으로 계산한다.

```text
이미지 존재   0.20
제목 존재     0.20
주소 존재     0.25
좌표 존재     0.25
전화번호 존재 0.10
```

상세정보 API를 붙이면 운영시간, 주차정보, 소개정보를 추가할 수 있다.

## 10. 다양성 재정렬

구현 클래스:

```text
DiversityRerankingService
```

최종 점수만 사용하면 같은 콘텐츠 타입이 반복될 수 있다.

현재는 이미 선택된 장소와 같은 `contentTypeId`가 반복될수록 감점한다.

```text
ReRankScore = FinalPlaceScore - sameContentTypeCount * 0.10
```

아직 `cat2`, `cat3`, 장소 벡터 간 유사도 패널티는 적용하지 않았다.

관광정보 API 상세/분류 필드를 확장하면 이 부분에 추가하면 된다.

## 11. 날짜별 코스 생성

구현 클래스:

```text
RouteOptimizationService
StayTimePolicy
```

여행스타일별 하루 추천 장소 수는 다음과 같다.

| 여행스타일 | 하루 장소 수 |
| --- | --- |
| RELAXED | 4 |
| NORMAL | 5 |
| TIGHT | 7 |

현재 방식은 다음과 같다.

1. 다양성 재정렬된 장소에서 `여행 일수 * 하루 장소 수`만큼 선택한다.
2. 경도 기준으로 정렬해 날짜별 버킷에 나눈다.
3. 각 날짜 안에서는 최종 점수가 가장 높은 장소를 시작점으로 둔다.
4. 이후 가장 가까운 좌표의 장소를 순서대로 붙인다.

이는 기획서의 Greedy Insertion을 단순화한 MVP 구현이다.

### 체류시간

`StayTimePolicy`가 콘텐츠 타입별 기본 체류시간을 반환한다.

| contentTypeId | 기본 체류시간 |
| --- | --- |
| 14 문화시설 | 120분 |
| 15 행사/축제 | 120분 |
| 28 레포츠 | 120분 |
| 38 쇼핑 | 90분 |
| 39 음식점 | 75분 |
| 기타 | 90분 |

## 12. 저장 정책

현재 추천 미리보기 API는 DB에 아무 것도 저장하지 않는다.

이 정책은 다음 원칙을 따른다.

```text
추천 요청 발생
→ 공공데이터 API 호출
→ 요청 메모리에서 가공
→ 추천 결과 생성
→ 외부 API 원본 응답 폐기
```

추후 사용자가 추천 결과를 확정하면 다음 데이터만 저장하는 방식이 적합하다.

- `contentId`
- 기존 `TouristSpot` 엔티티에 필요한 최소 장소 정보
- 기존 `CourseSpot` 엔티티의 날짜별 순서

## 13. 주요 확장 포인트

### 13.1 관광정보 API 고도화

현재는 `searchKeyword2`만 사용한다.

다음 API를 추가하면 추천 품질이 좋아진다.

- 지역 기반 목록 API
- 콘텐츠 타입별 목록 API
- 상세정보 API
- 소개정보 API
- 반복정보 API

추가 위치:

```text
TourApiClient
TourApiCandidateProvider
PlaceFeatureExtractor
PlaceQualityScoreCalculator
```

### 13.2 관광지 집중률

추가 위치:

```text
ContextualRankingService
CongestionScoreCalculator 신규 클래스
```

현재 `ContextualRankingService`의 중립값 `0.5`를 실제 계산 결과로 교체하면 된다.

### 13.3 지역별 방문자수

추가 위치:

```text
ContextualRankingService
RegionTrendScoreCalculator 신규 클래스
RouteOptimizationService
```

장소 점수뿐 아니라 날짜별 권역 선택에도 사용할 수 있다.

### 13.4 두루누비

추가 위치:

```text
DurunubiCandidateProvider 신규 클래스
RouteOptimizationService
RecommendedCourseResponse 확장
```

두루누비는 일반 장소 하나가 아니라 오전/오후 일정 전체를 차지하는 앵커 콘텐츠로 모델링하는 것이 좋다.

### 13.5 추천 결과 확정 저장

추가할 API:

```http
POST /api/travels/{travelId}/recommendations/apply
```

예상 흐름:

```text
사용자가 preview 결과 확인
  ↓
contentId와 날짜별 순서 제출
  ↓
TouristSpot 저장 또는 기존 엔티티 재사용
  ↓
CourseSpot 저장
```

기존 `TouristSpotService.saveSpotsToCourse()`와 `CourseService.updateCourseSpots()`의 책임을 재사용하거나, 추천 확정 전용 서비스를 별도로 두면 된다.

## 14. 현재 한계

- 키워드 검색 기반이라 지역/콘텐츠 타입 후보 생성의 정확도가 아직 낮다.
- 장소 상세정보를 호출하지 않아 소개정보, 운영시간, 주차정보 기반 점수는 반영하지 못한다.
- 실제 이동시간이 아니라 좌표 거리 기반으로만 순서를 정한다.
- 혼잡도와 지역 방문 추세는 아직 중립값이다.
- 추천 결과 확정 저장은 아직 구현되지 않았다.
- 동일 장소 벡터 유사도 기반 패널티는 아직 구현되지 않았다.

## 15. 클래스별 역할

### Controller

| 클래스 | 역할 |
| --- | --- |
| `CourseRecommendationController` | 추천 미리보기 API를 제공한다. `/api/travels/{travelId}/recommendations/preview` 요청을 받아 `CourseRecommendationService`에 위임한다. |

### DTO

| 클래스 | 역할 |
| --- | --- |
| `RecommendedCourseResponse` | 추천 결과 응답 DTO다. 여행 ID, 여행 일수, 날짜별 추천 장소 목록을 담는다. |
| `RecommendedDayResponse` | 하루 단위 추천 코스를 표현한다. 날짜, 일차, 장소 목록을 담는다. |
| `RecommendedPlaceResponse` | 추천 장소 한 개를 표현한다. 순서, 관광정보 API 식별자, 좌표, 체류시간, 주요 점수를 담는다. |

### Model

| 클래스 | 역할 |
| --- | --- |
| `RecommendationTag` | POZIT 추천 태그 8개를 enum으로 정의한다. 각 태그의 한국어 이름과 벡터 인덱스를 관리한다. |
| `CandidatePlace` | 관광정보 API 응답을 추천 로직에서 쓰기 좋은 내부 장소 모델로 변환한 객체다. |
| `PlaceFeatureVector` | 장소의 8차원 태그 점수 벡터를 감싸는 값 객체다. |
| `ScoredPlace` | 후보 장소와 콘텐츠 점수, 교통 점수, 품질 점수, 최종 점수를 함께 담는 모델이다. |
| `CourseRecommendCommand` | 추천 처리에 필요한 여행 조건을 하나로 묶은 command 객체다. |

### Service

| 클래스 | 역할 |
| --- | --- |
| `CourseRecommendationService` | 추천 프로세스 전체를 조립하는 애플리케이션 서비스다. 여행 조회, 권한 검증, 후보 조회, 점수 계산, 재정렬, 코스 생성을 순서대로 호출한다. |
| `TourApiCandidateProvider` | 여행 지역과 태그를 기반으로 관광정보 API 검색 키워드를 만들고 후보 장소를 수집한다. |
| `UserPreferenceVectorFactory` | 사용자가 선택한 태그 목록을 8차원 사용자 선호 벡터로 변환한다. |
| `PlaceFeatureExtractor` | 후보 장소의 `contentTypeId`와 제목 키워드를 기반으로 장소 콘텐츠 벡터를 생성한다. |
| `CosineSimilarityCalculator` | 사용자 벡터와 장소 벡터의 코사인 유사도를 계산한다. |
| `ContentScoreCalculator` | 코사인 유사도와 태그 균형 점수를 결합해 콘텐츠 점수를 계산한다. |
| `ContextualRankingService` | 콘텐츠 점수에 혼잡도, 교통수단, 지역 추세, 품질 점수를 반영해 최종 장소 점수를 계산한다. |
| `TransportationScoreCalculator` | 이동수단별 장소 적합도 점수를 계산한다. 현재는 도보, 자차, 대중교통에 대해 단순 규칙을 적용한다. |
| `PlaceQualityScoreCalculator` | 이미지, 제목, 주소, 좌표, 전화번호 존재 여부로 장소 정보 품질 점수를 계산한다. |
| `DiversityRerankingService` | 동일 `contentTypeId`가 반복되는 것을 줄이기 위해 재정렬 점수를 계산한다. |
| `RouteOptimizationService` | 재정렬된 장소를 여행 일수와 여행스타일에 맞게 날짜별로 나누고 방문 순서를 만든다. |
| `StayTimePolicy` | 콘텐츠 타입별 기본 체류시간을 반환한다. |

## 16. 검증 결과

컴파일 검증:

```bash
./gradlew compileJava
```

결과:

```text
BUILD SUCCESSFUL
```

전체 테스트:

```bash
./gradlew test
```

결과:

```text
FAILED
```

실패 원인:

```text
RedisConnectionFailureException
RedisCommandTimeoutException
```

새 추천 코드의 컴파일 오류가 아니라 테스트 환경에서 Redis 연결이 되지 않아 Spring context 로딩이 실패했다.
