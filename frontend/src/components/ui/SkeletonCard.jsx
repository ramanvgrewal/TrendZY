export default function SkeletonCard({ featured = false }) {
  return (
    <div className={`card-base ${featured ? 'w-full md:w-[600px] flex-shrink-0' : 'w-[280px] flex-shrink-0 flex flex-col'} h-[380px]`}>
      {/* Image Area Skeleton */}
      <div className={`bg-[#1a1a1a] animate-pulse ${featured ? 'h-full md:w-[300px] md:h-[380px]' : 'h-[200px] w-full'}`} />
      
      {/* Content Area Skeleton */}
      <div className="p-4 flex flex-col gap-4 flex-1">
        <div className="flex gap-2">
          <div className="bg-[#1a1a1a] animate-pulse h-4 w-16 rounded"></div>
          <div className="bg-[#1a1a1a] animate-pulse h-4 w-12 rounded"></div>
        </div>
        
        <div className="bg-[#1a1a1a] animate-pulse h-6 w-3/4 rounded mt-2"></div>
        <div className="bg-[#1a1a1a] animate-pulse h-4 w-1/2 rounded mt-1"></div>
        
        <div className="flex-1"></div>
        <div className="flex justify-between items-center mt-4">
          <div className="bg-[#1a1a1a] animate-pulse h-6 w-20 rounded"></div>
          <div className="bg-[#1a1a1a] animate-pulse h-8 w-24 rounded-full"></div>
        </div>
      </div>
    </div>
  );
}
