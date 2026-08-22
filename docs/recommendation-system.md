# POZIT 여행 코스 추천 시스템 구현 문서

## 1. 현재 구현 범위

현재 추천 시스템은 공공데이터 국문 관광정보 API를 이용해 여행별 추천 코스를 미리보기 형태로 생성한다.

추천 결과는 즉시 DB에 저장하지 않는다. 외부 API 원본 응답은 요청 처리 중 메모리에서만 사용하고, 최종 응답에는 POZIT에서 필요한 추천 결과만 담는다.

구현된 기능은 다음과 같다.

- 지역 기반 목록 API를 통한 후보 장소 생성
- 콘텐츠 타입별 목록 조회
- 키워드 검색 API를 이용한 후보 보충
- 상세정보 API를 이용한 장소 설명, 주소, 이미지, 분류 보강
- 소개정보 API를 이용한 운영시간, 주차, 행사 기간, 음식점 정보 보강
- 반복정보 API를 이용한 부가 설명 보강
- 사용자 태그 8차원 벡터 생성
- 장소 콘텐츠 8차원 벡터 생성
- 코사인 유사도 기반 콘텐츠 점수 계산
- 선택 태그 균형 점수 반영
- 교통수단 적합도 점수 반영
- 장소 정보 품질 점수 반영
- 다양성 재정렬
- 여행스타일별 날짜별 장소 수 배분
- 좌표 기반 방문 순서 생성

아직 구현되지 않은 항목은 다음과 같다.

- 관광지 집중률 API 기반 혼잡도 점수
- 지역별 방문자수 API 기반 지역 추세 점수
- 두루누비 걷기 코스 연동
- 실제 길찾기 API 기반 이동시간 계산
- 추천 결과 확정 저장 API

현재 `CongestionScore`와 `RegionTrendScore`는 중립값 `0.5`를 사용한다.

## 2. API

### 추천 미리보기

```http
POST /api/travels/{travelId}/recommendations/preview
```

해당 여행의 지역, 날짜, 여행스타일, 교통수단, 태그를 기반으로 날짜별 추천 코스를 생성한다.

요청 사용자는 해당 여행의 멤버여야 한다.

응답 예시:

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

### 추천 카드 미리보기

```http
POST /api/travels/{travelId}/recommendations/preview/card
```

추천 결과를 여행 카드 UI에 바로 표시할 수 있도록 카드 메타 정보와 원본 추천 코스를 함께 반환한다.
추천 결과는 DB에 저장하지 않는다.

응답 예시:

```json
{
  "isSuccess": true,
  "code": "COMMON200",
  "message": "요청에 성공했습니다.",
  "result": {
    "travelId": 1,
    "badge": "Pozit Pick!",
    "cardTitle": "8월 추천, 강릉은 어때요?",
    "travelTitle": "강릉 여행",
    "destination": "강릉",
    "startDate": "2026-08-01",
    "endDate": "2026-08-02",
    "dayCount": 2,
    "nightCount": 1,
    "periodText": "8/1 - 8/2 · 1박 2일",
    "thumbnailImageUrl": "https://...",
    "imageUrls": ["https://..."],
    "tags": ["문화", "힐링"],
    "memberCount": 2,
    "placeCount": 6,
    "previewPlaces": [
      {
        "dayNumber": 1,
        "orderIndex": 1,
        "title": "경포해변",
        "address": "강원특별자치도 강릉시 ...",
        "imageUrl": "https://..."
      }
    ],
    "recommendedCourse": {
      "travelId": 1,
      "dayCount": 2,
      "days": []
    }
  }
}
```

## 3. 전체 처리 흐름

```text
travelId 수신
  ↓
Travel 조회 및 여행 멤버 권한 검증
  ↓
TravelTag 조회
  ↓
CourseRecommendCommand 생성
  ↓
지역 기반 목록 API 호출
  ↓
태그별 콘텐츠 타입 목록 API 호출
  ↓
키워드 검색 API로 후보 보충
  ↓
상위 후보 상세정보/소개정보/반복정보 보강
  ↓
사용자 태그 벡터 생성
  ↓
장소 콘텐츠 벡터 생성
  ↓
콘텐츠 점수 계산
  ↓
교통수단/품질/중립 혼잡도/중립 지역추세 반영
  ↓
다양성 재정렬
  ↓
날짜별 배치 및 방문 순서 생성
  ↓
RecommendedCourseResponse 반환
```

## 4. 관광정보 API 활용 방식

### 4.1 지역 기반 목록 API

구현 위치:

```text
TourApiClient.findAreaBasedPlaces()
TourApiCandidateProvider.findCandidates()
```

사용 API:

```text
areaBasedList2
```

역할:

- 사용자가 선택한 지역의 기본 후보 풀 생성
- 키워드에 의존하지 않는 안정적인 장소 수집
- `regionCode`를 법정동 코드 파라미터로 변환해 조회

현재 지역 코드 처리:

```text
1. RegionRepository.existsByCode()로 POZIT 내부 regionCode 존재 여부 검증
2. regionCode 앞 2자리 → lDongRegnCd
3. regionCode가 시군구 코드이면 → lDongSignguCd
4. 광역 법정동 코드 → 관광정보 API areaCode fallback 매핑
5. API 응답도 법정동 코드 우선, 없으면 areaCode로 보조 검증
```

예:

```text
11000 → lDongRegnCd=11
11110 → lDongRegnCd=11, lDongSignguCd=11110
```

### 4.2 콘텐츠 타입별 목록 API

`areaBasedList2` 호출 시 `contentTypeId`를 함께 전달해 타입별 후보를 조회한다.

사용하는 콘텐츠 타입:

| contentTypeId | 의미 |
| --- | --- |
| `12` | 관광지 |
| `14` | 문화시설 |
| `15` | 행사/축제 |
| `28` | 레포츠 |
| `38` | 쇼핑 |
| `39` | 음식점 |

태그별 우선 조회 타입:

| 태그 | 우선 조회 contentTypeId |
| --- | --- |
| 기록 | `12`, `14` |
| 미식 | `39`, `38` |
| 힐링 | `12` |
| 체험 | `15`, `28`, `12` |
| 문화 | `14`, `12`, `15` |
| 예술 | `14`, `12`, `15` |
| 쇼핑 | `38` |
| 탐험 | `12`, `28` |

### 4.3 키워드 검색 API

사용 API:

```text
searchKeyword2
```

역할:

- 지역 기반 목록에서 부족한 후보 보충
- 태그 의도가 강한 장소를 추가 수집

태그별 검색어:

| 태그 | 검색어 |
| --- | --- |
| 기록 | `{destination} 관광지` |
| 미식 | `{destination} 맛집` |
| 힐링 | `{destination} 공원` |
| 체험 | `{destination} 체험` |
| 문화 | `{destination} 문화` |
| 예술 | `{destination} 미술관` |
| 쇼핑 | `{destination} 시장` |
| 탐험 | `{destination} 트레킹` |

### 4.4 상세정보 API

사용 API:

```text
detailCommon2
```

역할:

- 장소명, 주소, 좌표, 이미지, 분류코드 보강
- `overview` 기반 장소 설명 보강
- 품질 점수 계산에 활용
- 콘텐츠 벡터 생성 시 소개 문장 키워드 분석에 활용

현재는 후보 전체가 아니라 상위 후보 최대 40개만 상세 조회한다.

### 4.5 소개정보 API

사용 API:

```text
detailIntro2
```

역할:

- 운영시간, 쉬는 날, 주차 정보 보강
- 음식점 대표 메뉴, 영업시간, 주차 정보 보강
- 행사 시작일/종료일 보강
- 체험 안내 정보 보강

추천 로직 활용:

- 종료된 행사 제외
- 주차 정보가 있으면 자차 점수 가점
- 운영정보가 있으면 품질 점수 가점
- 체험 정보가 있으면 체험 태그 점수 보정
- 메뉴 정보가 있으면 미식 태그 점수 보정

### 4.6 반복정보 API

사용 API:

```text
detailInfo2
```

역할:

- 장소별 반복 부가정보 보강
- 전시/행사/코스/부대시설 등의 설명 키워드 보강

현재는 `detailInfo2`의 여러 반복정보를 모두 읽고, `infoname + infotext`를 중복 제거 후 하나의 `repeatedInfo`로 병합해 콘텐츠 벡터 생성에 사용한다.

## 5. 후보 필터링

다음 후보는 제외한다.

- `contentId`가 없는 장소
- 제목이 없는 장소
- 좌표가 없는 장소
- 선택 지역 코드와 맞지 않는 장소
- 동일 `contentId` 중복 장소
- 여행 시작일 전에 종료된 행사

관광정보 API에서 결과가 없을 때 `items`가 빈 문자열 `""`로 내려오는 경우가 있어, `TourApiClient`에서 `"items": ""`를 `"items": null`로 보정한다.

## 6. 사용자 태그 벡터

구현 위치:

```text
RecommendationTag
UserPreferenceVectorFactory
```

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

선택된 태그는 `1.0`, 선택되지 않은 태그는 `0.0`이다.

## 7. 장소 콘텐츠 벡터

구현 위치:

```text
PlaceFeatureExtractor
```

장소 벡터는 다음 정보를 이용한다.

- `contentTypeId`
- `cat1`, `cat2`, `cat3`
- 장소명
- `overview`
- 체험 정보
- 메뉴 정보
- 반복정보

현재 기본 점수는 `contentTypeId`를 중심으로 부여하고, 분류코드와 텍스트 키워드로 보정한다.

예:

```text
contentTypeId=39 음식점
→ 미식 1.0, 체험 0.2

장소 텍스트에 "시장", "먹거리", "맛집", "대표메뉴" 포함
→ 미식 추가 보정

장소 텍스트에 "체험", "공방", "만들기" 포함
→ 체험 추가 보정
```

모든 태그 점수는 최대 `1.0`으로 제한한다.

## 8. 콘텐츠 점수

구현 위치:

```text
CosineSimilarityCalculator
ContentScoreCalculator
```

콘텐츠 점수는 코사인 유사도와 태그 균형 점수를 결합한다.

```text
ContentScore = CosineSimilarity * 0.85 + TagBalanceScore * 0.15
```

`TagBalanceScore`는 선택된 태그들의 장소 점수 중 최솟값이다.

```text
TagBalanceScore = min(선택 태그별 장소 점수)
```

## 9. 최종 장소 점수

구현 위치:

```text
ContextualRankingService
TransportationScoreCalculator
PlaceQualityScoreCalculator
```

현재 산식:

```text
FinalPlaceScore
= ContentScore * 0.50
+ CongestionScore * 0.20
+ TransportationScore * 0.15
+ RegionTrendScore * 0.10
+ QualityScore * 0.05
```

현재 상태:

```text
CongestionScore = 0.5
RegionTrendScore = 0.5
```

### 교통수단 점수

| 교통수단 | 현재 규칙 |
| --- | --- |
| WALK | 좌표가 있으면 기본 가점, 레포츠는 추가 가점 |
| CAR | 주차 정보가 있으면 높은 가점 |
| PUBLIC | 주소와 좌표가 있으면 기본 가점 |

### 품질 점수

현재 품질 점수:

```text
이미지 존재       0.20
소개정보 존재     0.25
운영정보 존재     0.20
주소와 좌표 존재  0.25
전화/홈페이지/주차 0.10
```

## 10. 다양성 재정렬

구현 위치:

```text
DiversityRerankingService
```

같은 `contentTypeId`, `cat2`, `cat3`가 반복될수록 감점한다.

```text
ReRankScore
= FinalPlaceScore
- sameContentTypeCount * 0.10
- sameCat2Count * 0.08
- sameCat3Count * 0.05
```

아직 장소 벡터 유사도 기반 패널티는 적용하지 않았다.

## 11. 날짜별 코스 생성

구현 위치:

```text
RouteOptimizationService
StayTimePolicy
```

여행스타일별 하루 장소 수:

| 여행스타일 | 하루 장소 수 |
| --- | --- |
| RELAXED | 4 |
| NORMAL | 5 |
| TIGHT | 7 |

현재 날짜 배정 방식:

```text
1. 추천 상위 장소를 여행 일수 * 하루 장소 수만큼 선택
2. 경도 기준으로 정렬
3. 하루 장소 수만큼 끊어 날짜별 배정
```

현재 하루 안 방문 순서:

```text
1. 해당 날짜 장소 중 finalScore가 가장 높은 장소를 시작점으로 선택
2. 현재 장소에서 좌표상 가장 가까운 장소를 다음 장소로 선택
3. 모든 장소가 배치될 때까지 반복
```

이는 실제 이동시간 기반 최적화가 아니라 MVP용 좌표 기반 근사 방식이다.

## 12. 클래스별 역할

### Controller

| 클래스 | 역할 |
| --- | --- |
| `CourseRecommendationController` | 추천 미리보기 API를 제공한다. |

### DTO

| 클래스 | 역할 |
| --- | --- |
| `RecommendedCourseResponse` | 추천 결과 최상위 응답 DTO다. |
| `RecommendedDayResponse` | 하루 단위 추천 코스를 표현한다. |
| `RecommendedPlaceResponse` | 추천 장소 한 개와 주요 점수를 표현한다. |

### Model

| 클래스 | 역할 |
| --- | --- |
| `RecommendationTag` | POZIT 추천 태그 8개와 벡터 인덱스를 정의한다. |
| `CandidatePlace` | 관광정보 API 응답을 추천 내부 장소 모델로 변환한다. 상세/소개/반복정보를 병합할 수 있다. |
| `PlaceFeatureVector` | 장소의 8차원 태그 점수 벡터 값 객체다. |
| `ScoredPlace` | 장소와 계산된 점수들을 함께 담는다. |
| `CourseRecommendCommand` | 추천 처리에 필요한 여행 조건을 묶는다. |

### Service

| 클래스 | 역할 |
| --- | --- |
| `CourseRecommendationService` | 추천 프로세스 전체를 조립한다. |
| `TourApiCandidateProvider` | 지역 기반, 타입별, 키워드, 상세/소개/반복정보 API를 이용해 후보를 생성하고 보강한다. |
| `UserPreferenceVectorFactory` | 사용자 태그를 선호 벡터로 변환한다. |
| `PlaceFeatureExtractor` | 후보 장소를 콘텐츠 벡터로 변환한다. |
| `CosineSimilarityCalculator` | 코사인 유사도를 계산한다. |
| `ContentScoreCalculator` | 콘텐츠 점수를 계산한다. |
| `ContextualRankingService` | 최종 장소 점수를 계산한다. |
| `TransportationScoreCalculator` | 교통수단 적합도를 계산한다. |
| `PlaceQualityScoreCalculator` | 장소 정보 품질 점수를 계산한다. |
| `DiversityRerankingService` | 동일 콘텐츠 타입 반복을 줄인다. |
| `RouteOptimizationService` | 날짜별 장소 배치와 방문 순서를 만든다. |
| `StayTimePolicy` | 콘텐츠 타입별 기본 체류시간을 반환한다. |

## 13. 다음 개선 과제

- 관광지 집중률 API 연동
- 지역별 방문자수 API 연동
- 두루누비 API 연동
- 지도 길찾기 API 기반 실제 이동시간 반영
- 추천 결과 확정 저장 API 추가
- 관광정보 API `sigunguCode` 정밀 매핑 테이블 추가
- 장소 벡터 유사도 기반 다양성 패널티 추가

## 14. 검증 결과

컴파일 검증:

```bash
./gradlew compileJava
```

결과:

```text
BUILD SUCCESSFUL
```
