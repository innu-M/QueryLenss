# UML Class Diagram

The editable UML class diagram is maintained in Lucidchart:

[Open the QueryLens UML Class Diagram](https://lucid.app/lucidchart/ccb6834d-bdaa-4e59-8a80-f4f87d2f175d/edit)

It documents the original design decisions: query execution, SQL analysis and plan inspection, strategy-based recommendations, observer-based persistence, repositories, and recommendation state transitions.

The alternative-comparison design is also maintained here as editable Mermaid source:

```mermaid
classDiagram
    class Command~T~ {
        <<interface>>
        +execute() T
    }
    class CompareAlternativesCommand
    class AlternativeComparisonService {
        +compare(databasePath, sql) ComparisonReport
        +subscribe(observer)
    }
    class AlternativeQueryGenerator
    class QueryRewriteStrategy {
        <<interface>>
        +supports(sql, analysis) boolean
        +generate(connection, sql, analysis) List~QueryCandidate~
    }
    class IndexPlanStrategy
    class CandidateValidationChain
    class CandidateValidator {
        <<abstract>>
        +linkWith(next) CandidateValidator
        +validate(candidate) ValidationResult
    }
    class QueryBenchmarkTemplate {
        <<abstract>>
        +benchmark(connection, candidate) RawBenchmarkResult
    }
    class SQLiteSelectBenchmark
    class RankingStrategy {
        <<interface>>
        +rank(candidates) List~CandidateComparison~
    }
    class MedianTimeRankingStrategy
    class PlanExplanationService
    class BenchmarkPublisher
    class BenchmarkObserver {
        <<interface>>
    }
    class BenchmarkPersistenceObserver
    class ComparisonHistoryRepository

    Command~T~ <|.. CompareAlternativesCommand
    CompareAlternativesCommand --> AlternativeComparisonService
    AlternativeComparisonService --> AlternativeQueryGenerator
    AlternativeQueryGenerator o-- QueryRewriteStrategy
    QueryRewriteStrategy <|.. IndexPlanStrategy
    AlternativeComparisonService --> CandidateValidationChain
    CandidateValidationChain o-- CandidateValidator
    AlternativeComparisonService --> QueryBenchmarkTemplate
    QueryBenchmarkTemplate <|-- SQLiteSelectBenchmark
    AlternativeComparisonService --> RankingStrategy
    RankingStrategy <|.. MedianTimeRankingStrategy
    AlternativeComparisonService --> PlanExplanationService
    AlternativeComparisonService --> BenchmarkPublisher
    BenchmarkPublisher o-- BenchmarkObserver
    BenchmarkObserver <|.. BenchmarkPersistenceObserver
    BenchmarkPersistenceObserver --> ComparisonHistoryRepository
```
